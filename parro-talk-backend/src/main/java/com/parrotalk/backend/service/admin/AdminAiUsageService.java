package com.parrotalk.backend.service.admin;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parrotalk.backend.dto.admin.AiUsageByFeatureDto;
import com.parrotalk.backend.dto.admin.AiUsageByModelDto;
import com.parrotalk.backend.dto.admin.AiUsageByUserDto;
import com.parrotalk.backend.dto.admin.AiUsageDailyPointDto;
import com.parrotalk.backend.dto.admin.AiUsageSummaryDto;
import com.parrotalk.backend.dto.admin.AiUsageUserPageDto;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.AiUsageStatsRepository;
import com.parrotalk.backend.repository.AiUsageStatsRepository.UserRow;

import lombok.RequiredArgsConstructor;

/** Admin read-side aggregation over AI usage events. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAiUsageService {

    public static final String DEFAULT_TZ = "Asia/Ho_Chi_Minh";
    public static final int MAX_RANGE_DAYS = 30;
    public static final int DEFAULT_RANGE_DAYS = 7;
    public static final int MAX_PAGE_SIZE = 100;
    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    private final AiUsageStatsRepository repository;

    /** Resolved, validated request filter. */
    public record Filter(Instant from, Instant to, ZoneId zone, String userId, boolean systemOnly,
            String model, String provider, String feature) {
    }

    public Filter buildFilter(String from, String to, String tz, String userId,
            String model, String provider, String feature) {
        ZoneId zone = parseZone(tz);
        Instant toInstant = (to == null || to.isBlank()) ? Instant.now() : parseBound(to, zone, true);
        Instant fromInstant = (from == null || from.isBlank())
                ? toInstant.minus(Duration.ofDays(DEFAULT_RANGE_DAYS))
                : parseBound(from, zone, false);
        if (!toInstant.isAfter(fromInstant)) {
            throw bad("'to' must be after 'from'.");
        }
        if (Duration.between(fromInstant, toInstant).compareTo(Duration.ofDays(MAX_RANGE_DAYS)) > 0) {
            throw bad("Range must not exceed " + MAX_RANGE_DAYS + " days.");
        }
        boolean systemOnly = false;
        String userFilter = null;
        if (userId != null && !userId.isBlank()) {
            if ("system".equalsIgnoreCase(userId.trim())) {
                systemOnly = true;
            } else {
                try {
                    userFilter = UUID.fromString(userId.trim()).toString();
                } catch (IllegalArgumentException e) {
                    throw bad("Invalid userId.");
                }
            }
        }
        return new Filter(fromInstant, toInstant, zone, userFilter, systemOnly,
                blankToNull(model), blankToNull(provider), blankToNull(feature));
    }

    public AiUsageSummaryDto summary(Filter f) {
        var row = repository.summary(f.from(), f.to(), f.userId(), f.systemOnly(), f.model(), f.provider(), f.feature());
        long requests = nz(row == null ? null : row.getRequests());
        long in = nz(row == null ? null : row.getInputTokens());
        long out = nz(row == null ? null : row.getOutputTokens());
        long active = nz(row == null ? null : row.getActiveUsers());
        double avg = requests == 0 ? 0 : round1((double) (in + out) / requests);
        return new AiUsageSummaryDto(f.from(), f.to(), requests, in, out, in + out, active, avg);
    }

    public List<AiUsageDailyPointDto> timeseries(Filter f) {
        var rows = repository.daily(f.from(), f.to(), f.zone().getId(), f.userId(), f.systemOnly(),
                f.model(), f.provider(), f.feature());
        Map<LocalDate, AiUsageStatsRepository.DailyRow> byDay = new java.util.HashMap<>();
        for (var r : rows) {
            byDay.put(toLocalDate(r.getDay()), r);
        }
        LocalDate start = f.from().atZone(f.zone()).toLocalDate();
        LocalDate end = f.to().minusNanos(1).atZone(f.zone()).toLocalDate();
        List<AiUsageDailyPointDto> out = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            var r = byDay.get(d);
            long in = r == null ? 0 : nz(r.getInputTokens());
            long o = r == null ? 0 : nz(r.getOutputTokens());
            out.add(new AiUsageDailyPointDto(d, r == null ? 0 : nz(r.getRequests()), in, o, in + o));
        }
        return out;
    }

    public List<AiUsageByModelDto> byModel(Filter f) {
        var rows = repository.byModel(f.from(), f.to(), f.userId(), f.systemOnly(), f.model(), f.provider(), f.feature());
        long total = rows.stream().mapToLong(r -> nz(r.getInputTokens()) + nz(r.getOutputTokens())).sum();
        return rows.stream().map(r -> {
            long in = nz(r.getInputTokens());
            long o = nz(r.getOutputTokens());
            return new AiUsageByModelDto(r.getProvider(), r.getModel(), nz(r.getRequests()), in, o, in + o,
                    share(in + o, total));
        }).toList();
    }

    public List<AiUsageByFeatureDto> byFeature(Filter f) {
        var rows = repository.byFeature(f.from(), f.to(), f.userId(), f.systemOnly(), f.model(), f.provider(), f.feature());
        long total = rows.stream().mapToLong(r -> nz(r.getInputTokens()) + nz(r.getOutputTokens())).sum();
        return rows.stream().map(r -> {
            long in = nz(r.getInputTokens());
            long o = nz(r.getOutputTokens());
            return new AiUsageByFeatureDto(r.getFeature(), nz(r.getRequests()), in, o, in + o, share(in + o, total));
        }).toList();
    }

    public AiUsageUserPageDto byUser(Filter f, int page, int size, String search, String sort) {
        if (page < 0) {
            throw bad("page must be >= 0.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw bad("size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }
        Comparator<AiUsageByUserDto> cmp = comparator(sort);
        String pattern = toLikePattern(search);
        List<AiUsageByUserDto> all = repository.byUser(f.from(), f.to(), f.userId(), f.systemOnly(),
                f.model(), f.provider(), f.feature(), pattern).stream().map(this::toUserDto)
                .sorted(cmp).toList();
        long total = all.size();
        int fromIdx = (int) Math.min((long) page * size, total);
        int toIdx = (int) Math.min(fromIdx + (long) size, total);
        int totalPages = (int) ((total + size - 1) / size);
        return new AiUsageUserPageDto(all.subList(fromIdx, toIdx), page, size, total, totalPages);
    }

    private AiUsageByUserDto toUserDto(UserRow r) {
        long in = nz(r.getInputTokens());
        long o = nz(r.getOutputTokens());
        String display;
        if (r.getUserId() == null) {
            display = "System";
        } else if (r.getFullName() != null && !r.getFullName().isBlank()) {
            display = r.getFullName();
        } else {
            display = r.getUsername();
        }
        return new AiUsageByUserDto(r.getUserId(), r.getEmail(), display, Boolean.TRUE.equals(r.getDeleted()),
                nz(r.getRequests()), in, o, in + o, toInstant(r.getLastUsedAt()));
    }

    private Comparator<AiUsageByUserDto> comparator(String sort) {
        String field = "totalTokens";
        boolean desc = true;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            field = parts[0].trim();
            desc = false;
            if (parts.length > 1) {
                String dir = parts[1].trim().toLowerCase(Locale.ROOT);
                if (dir.equals("desc")) {
                    desc = true;
                } else if (!dir.equals("asc")) {
                    throw bad("Invalid sort direction.");
                }
            }
        }
        Comparator<AiUsageByUserDto> base = switch (field) {
            case "totalTokens" -> Comparator.comparingLong(AiUsageByUserDto::totalTokens);
            case "inputTokens" -> Comparator.comparingLong(AiUsageByUserDto::inputTokens);
            case "outputTokens" -> Comparator.comparingLong(AiUsageByUserDto::outputTokens);
            case "requests" -> Comparator.comparingLong(AiUsageByUserDto::requests);
            case "lastUsedAt" -> Comparator.comparing(AiUsageByUserDto::lastUsedAt,
                    Comparator.nullsFirst(Comparator.naturalOrder()));
            case "email" -> Comparator.comparing(
                    (AiUsageByUserDto d) -> d.email() == null ? "" : d.email().toLowerCase(Locale.ROOT));
            default -> throw bad("Unsupported sort field: " + field);
        };
        // Stable tie-break so paging is deterministic.
        Comparator<AiUsageByUserDto> withDir = desc ? base.reversed() : base;
        return withDir.thenComparing(d -> String.valueOf(d.userId()));
    }

    static String toLikePattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String s = search.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + s + "%";
    }

    private static ZoneId parseZone(String tz) {
        try {
            return ZoneId.of(tz == null || tz.isBlank() ? DEFAULT_TZ : tz.trim());
        } catch (java.time.DateTimeException e) {
            throw bad("Invalid tz.");
        }
    }

    private static Instant parseBound(String value, ZoneId zone, boolean endOfDay) {
        String v = value.trim();
        try {
            if (v.length() == 10) {
                LocalDate d = LocalDate.parse(v);
                return (endOfDay ? d.plusDays(1) : d).atStartOfDay(zone).toInstant();
            }
            try {
                return Instant.parse(v);
            } catch (DateTimeParseException e) {
                return OffsetDateTime.parse(v).toInstant();
            }
        } catch (DateTimeParseException e) {
            throw bad("Invalid date/time value: " + v);
        }
    }

    private static LocalDate toLocalDate(Object o) {
        if (o instanceof LocalDate d) {
            return d;
        }
        if (o instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (o instanceof java.util.Date d) {
            return new java.sql.Date(d.getTime()).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(o));
    }

    private static Instant toInstant(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Instant i) {
            return i;
        }
        if (o instanceof OffsetDateTime odt) {
            return odt.toInstant();
        }
        if (o instanceof ZonedDateTime z) {
            return z.toInstant();
        }
        if (o instanceof Timestamp t) {
            return t.toInstant();
        }
        if (o instanceof java.util.Date d) {
            return d.toInstant();
        }
        return Instant.parse(String.valueOf(o));
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    private static double share(long part, long total) {
        return total == 0 ? 0 : round1(part * 100.0 / total);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static ParroTalkException bad(String message) {
        return new ParroTalkException(message, VALIDATION_ERROR, HttpStatus.BAD_REQUEST);
    }
}
