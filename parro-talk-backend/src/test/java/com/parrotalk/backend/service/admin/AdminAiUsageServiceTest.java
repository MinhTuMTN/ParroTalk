package com.parrotalk.backend.service.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.parrotalk.backend.dto.admin.AiUsageDailyPointDto;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.AiUsageStatsRepository;
import com.parrotalk.backend.repository.AiUsageStatsRepository.DailyRow;
import com.parrotalk.backend.repository.AiUsageStatsRepository.ModelRow;
import com.parrotalk.backend.repository.AiUsageStatsRepository.SummaryRow;
import com.parrotalk.backend.repository.AiUsageStatsRepository.UserRow;

class AdminAiUsageServiceTest {

    private AiUsageStatsRepository repo;
    private AdminAiUsageService service;

    @BeforeEach
    void setUp() {
        repo = mock(AiUsageStatsRepository.class);
        service = new AdminAiUsageService(repo);
    }

    private AdminAiUsageService.Filter filter(String from, String to) {
        return service.buildFilter(from, to, null, null, null, null, null);
    }

    @Test
    void defaultsToLast7Days() {
        var f = filter(null, null);
        assertEquals(7, java.time.Duration.between(f.from(), f.to()).toDays());
        assertEquals("Asia/Ho_Chi_Minh", f.zone().getId());
    }

    @Test
    void rejectsInvalidRanges() {
        assertThrows(ParroTalkException.class, () -> filter("2026-11-10T00:00:00Z", "2026-11-10T00:00:00Z"));
        assertThrows(ParroTalkException.class, () -> filter("2026-11-10T00:00:00Z", "2026-11-09T00:00:00Z"));
        assertThrows(ParroTalkException.class, () -> filter("2026-10-01T00:00:00Z", "2026-11-09T00:00:00Z"));
        assertThrows(ParroTalkException.class, () -> service.buildFilter(null, null, "Nope/Zone", null, null, null, null));
        assertThrows(ParroTalkException.class, () -> service.buildFilter(null, null, null, "not-a-uuid", null, null, null));
        assertThrows(ParroTalkException.class, () -> filter("garbage", null));
    }

    @Test
    void exactly30DaysAllowed() {
        assertDoesNotThrow(() -> filter("2026-10-10T00:00:00Z", "2026-11-09T00:00:00Z"));
    }

    @Test
    void userIdSystemAndUuid() {
        var sys = service.buildFilter(null, null, null, "system", null, null, null);
        assertTrue(sys.systemOnly());
        assertNull(sys.userId());
        UUID id = UUID.randomUUID();
        var u = service.buildFilter(null, null, null, id.toString(), " m ", "", null);
        assertEquals(id.toString(), u.userId());
        assertEquals("m", u.model());
        assertNull(u.provider());
    }

    @Test
    void bareDatesAreInclusiveOfToDayInTimezone() {
        var f = filter("2026-11-14", "2026-11-14");
        // Asia/Ho_Chi_Minh is UTC+7
        assertEquals(Instant.parse("2026-11-13T17:00:00Z"), f.from());
        assertEquals(Instant.parse("2026-11-14T17:00:00Z"), f.to());
    }

    @Test
    void timeseriesZeroFillsMissingDays() {
        var f = filter("2026-11-14", "2026-11-16");
        DailyRow row = mock(DailyRow.class);
        when(row.getDay()).thenReturn(java.sql.Date.valueOf("2026-11-15"));
        when(row.getRequests()).thenReturn(2L);
        when(row.getInputTokens()).thenReturn(10L);
        when(row.getOutputTokens()).thenReturn(5L);
        when(repo.daily(any(), any(), eq("Asia/Ho_Chi_Minh"), any(), anyBoolean(), any(), any(), any()))
                .thenReturn(List.of(row));
        List<AiUsageDailyPointDto> pts = service.timeseries(f);
        assertEquals(3, pts.size());
        assertEquals(LocalDate.parse("2026-11-14"), pts.get(0).date());
        assertEquals(0, pts.get(0).totalTokens());
        assertEquals(15, pts.get(1).totalTokens());
        assertEquals(0, pts.get(2).requests());
    }

    @Test
    void summaryHandlesEmptyAndComputesAverage() {
        var f = filter(null, null);
        var s0 = service.summary(f);
        assertEquals(0, s0.totalRequests());
        assertEquals(0.0, s0.avgTokensPerRequest());

        SummaryRow row = mock(SummaryRow.class);
        when(row.getRequests()).thenReturn(4L);
        when(row.getInputTokens()).thenReturn(300L);
        when(row.getOutputTokens()).thenReturn(100L);
        when(row.getActiveUsers()).thenReturn(2L);
        when(repo.summary(any(), any(), any(), anyBoolean(), any(), any(), any())).thenReturn(row);
        var s = service.summary(f);
        assertEquals(400, s.totalTokens());
        assertEquals(100.0, s.avgTokensPerRequest());
        assertEquals(2, s.activeUsers());
    }

    @Test
    void byModelComputesShare() {
        ModelRow a = mock(ModelRow.class);
        when(a.getModel()).thenReturn("a");
        when(a.getInputTokens()).thenReturn(75L);
        when(a.getOutputTokens()).thenReturn(0L);
        ModelRow b = mock(ModelRow.class);
        when(b.getModel()).thenReturn("b");
        when(b.getInputTokens()).thenReturn(25L);
        when(b.getOutputTokens()).thenReturn(0L);
        when(repo.byModel(any(), any(), any(), anyBoolean(), any(), any(), any())).thenReturn(List.of(a, b));
        var res = service.byModel(filter(null, null));
        assertEquals(75.0, res.get(0).sharePercent());
        assertEquals(25.0, res.get(1).sharePercent());
    }

    private UserRow user(UUID id, String email, String name, boolean deleted, long total, Object last) {
        UserRow r = mock(UserRow.class);
        when(r.getUserId()).thenReturn(id);
        when(r.getEmail()).thenReturn(email);
        when(r.getFullName()).thenReturn(name);
        when(r.getDeleted()).thenReturn(deleted);
        when(r.getInputTokens()).thenReturn(total);
        when(r.getOutputTokens()).thenReturn(0L);
        when(r.getRequests()).thenReturn(1L);
        when(r.getLastUsedAt()).thenReturn(last);
        return r;
    }

    @Test
    void byUserSortsPagesAndLabelsSystemAndDeleted() {
        List<UserRow> rows = List.of(
                user(UUID.randomUUID(), "a@x.com", "A", false, 10, java.sql.Timestamp.from(Instant.parse("2026-11-20T00:00:00Z"))),
                user(UUID.randomUUID(), "b@x.com", "B", true, 30, Instant.parse("2026-11-21T00:00:00Z")),
                user(null, null, null, false, 20, null));
        when(repo.byUser(any(), any(), any(), anyBoolean(), any(), any(), any(), any())).thenReturn(rows);
        var f = filter(null, null);
        var page = service.byUser(f, 0, 2, null, null);
        assertEquals(3, page.totalElements());
        assertEquals(2, page.totalPages());
        assertEquals("b@x.com", page.items().get(0).email());
        assertTrue(page.items().get(0).deleted());
        assertEquals("System", page.items().get(1).displayName());
        var p2 = service.byUser(f, 1, 2, null, "totalTokens,asc");
        assertEquals(1, p2.items().size());
        assertEquals("b@x.com", p2.items().get(0).email());
        assertEquals(Instant.parse("2026-11-20T00:00:00Z"),
                service.byUser(f, 0, 10, null, "email,asc").items().get(1).lastUsedAt());
        assertThrows(ParroTalkException.class, () -> service.byUser(f, 0, 101, null, null));
        assertThrows(ParroTalkException.class, () -> service.byUser(f, 0, 10, null, "bogus,desc"));
        assertThrows(ParroTalkException.class, () -> service.byUser(f, -1, 10, null, null));
    }

    @Test
    void searchPatternEscapesWildcards() {
        assertEquals("%a\\%b\\_c%", AdminAiUsageService.toLikePattern(" A%B_c "));
        assertNull(AdminAiUsageService.toLikePattern("  "));
    }
}
