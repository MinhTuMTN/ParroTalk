package com.parrotalk.backend.service.dictation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.parrotalk.backend.constant.DictationMode;
import com.parrotalk.backend.dto.dictation.BlankSlotDto;
import com.parrotalk.backend.dto.dictation.SegmentBlanksPayload;
import com.parrotalk.backend.service.dictation.DictationBlankConfig.BlankRange;
import com.parrotalk.backend.service.dictation.DictationBlankConfig.ModeWeights;
import com.parrotalk.backend.service.dictation.DictationLexicalEnricher.LexicalWordInfo;
import com.parrotalk.backend.service.dictation.DictationTokenizationService.DictationToken;

import lombok.RequiredArgsConstructor;

/**
 * Deterministic rule-and-lexicon-based blank generator for Fill-in-the-Blanks dictation.
 * Zero randomness, zero runtime AI calls.
 */
@Service
@RequiredArgsConstructor
public class DictationBlankGenerator {

    private final DictationTokenizationService tokenizationService;
    private final DictationLexicalEnricher lexicalEnricher;

    /**
     * Generates blanks for a single segment across EASY, MEDIUM, and HARD modes.
     */
    public SegmentBlanksPayload generateForSegment(String segmentText) {
        List<SegmentBlanksPayload> payloads = generateForSegments(List.of(segmentText == null ? "" : segmentText));
        return payloads.isEmpty() ? emptyPayload(0) : payloads.get(0);
    }

    /**
     * Batch-generates blank payloads for multiple segments using a single vocabulary lookup batch.
     */
    public List<SegmentBlanksPayload> generateForSegments(List<String> segmentTexts) {
        if (segmentTexts == null || segmentTexts.isEmpty()) {
            return List.of();
        }

        List<List<DictationToken>> tokenizedWordsPerSegment = new ArrayList<>(segmentTexts.size());
        List<DictationToken> allWordTokens = new ArrayList<>();
        for (String text : segmentTexts) {
            List<DictationToken> wordTokens = tokenizationService.extractWordTokens(text);
            tokenizedWordsPerSegment.add(wordTokens);
            allWordTokens.addAll(wordTokens);
        }

        Map<String, LexicalWordInfo> lexMap = lexicalEnricher.enrichTokens(allWordTokens);
        List<SegmentBlanksPayload> payloads = new ArrayList<>(segmentTexts.size());

        for (int i = 0; i < segmentTexts.size(); i++) {
            String text = segmentTexts.get(i);
            List<DictationToken> words = tokenizedWordsPerSegment.get(i);
            payloads.add(buildPayloadForSegment(text, words, lexMap));
        }
        return payloads;
    }

    private SegmentBlanksPayload buildPayloadForSegment(
            String rawText,
            List<DictationToken> words,
            Map<String, LexicalWordInfo> lexMap) {
        int wordCount = words.size();
        if (!isSuitableSegment(rawText, words)) {
            return emptyPayload(wordCount);
        }

        Map<DictationMode, List<BlankSlotDto>> levels = new EnumMap<>(DictationMode.class);
        for (DictationMode mode : List.of(DictationMode.EASY, DictationMode.MEDIUM, DictationMode.HARD)) {
            levels.put(mode, selectBlanksForMode(words, lexMap, mode));
        }

        return SegmentBlanksPayload.builder()
                .v(DictationBlankConfig.SCHEMA_VERSION)
                .wordCount(wordCount)
                .suitable(true)
                .levels(levels)
                .build();
    }

    private SegmentBlanksPayload emptyPayload(int wordCount) {
        Map<DictationMode, List<BlankSlotDto>> levels = new EnumMap<>(DictationMode.class);
        levels.put(DictationMode.EASY, List.of());
        levels.put(DictationMode.MEDIUM, List.of());
        levels.put(DictationMode.HARD, List.of());
        return SegmentBlanksPayload.builder()
                .v(DictationBlankConfig.SCHEMA_VERSION)
                .wordCount(wordCount)
                .suitable(false)
                .levels(levels)
                .build();
    }

    boolean isSuitableSegment(String rawText, List<DictationToken> words) {
        if (rawText == null || rawText.isBlank() || words.size() <= 1) {
            return false;
        }
        String trimmed = rawText.trim();
        if (trimmed.matches("^\\[[^\\]]+\\]$") || trimmed.matches("^\\([^)]+\\)$")) {
            return false;
        }
        if (words.size() == 2) {
            boolean allFillersOrInterjections = words.stream()
                    .allMatch(w -> EnglishLexiconProfiles.INTERJECTION_SINGLE_WORDS.contains(w.getNormalized()));
            if (allFillersOrInterjections) {
                return false;
            }
        }
        return true;
    }

    private List<BlankSlotDto> selectBlanksForMode(
            List<DictationToken> words,
            Map<String, LexicalWordInfo> lexMap,
            DictationMode mode) {
        int wordCount = words.size();
        BlankRange range = DictationBlankConfig.resolveBlankRange(wordCount, mode);
        if (range.max() <= 0) {
            return List.of();
        }

        int targetCount = computeTargetBlankCount(words, range, mode);
        List<WordAnalysis> analyses = analyzeWords(words, lexMap);

        Map<Integer, Double> baseScores = new HashMap<>(wordCount);
        for (WordAnalysis analysis : analyses) {
            baseScores.put(analysis.wordIndex(), scoreToken(analysis, mode, wordCount));
        }

        int maxConsecutive = DictationBlankConfig.maxConsecutiveBlanks(mode);
        List<Integer> selectedIndices = greedySelect(
                analyses, baseScores, targetCount, maxConsecutive, mode, wordCount);

        if (selectedIndices.size() < range.min()) {
            int relaxedConsecutive = DictationBlankConfig.fallbackMaxConsecutiveBlanks(mode);
            selectedIndices = greedySelect(
                    analyses, baseScores, targetCount, relaxedConsecutive, mode, wordCount);
        }

        selectedIndices.sort(Integer::compareTo);
        List<BlankSlotDto> slots = new ArrayList<>(selectedIndices.size());
        for (int wordIdx : selectedIndices) {
            DictationToken token = words.get(wordIdx);
            slots.add(BlankSlotDto.builder()
                    .wordIndex(wordIdx)
                    .charLength(token.getText().length())
                    .build());
        }
        return slots;
    }

    private int computeTargetBlankCount(List<DictationToken> words, BlankRange range, DictationMode mode) {
        if (range.min() == range.max()) {
            return range.max();
        }
        int contentCount = 0;
        for (DictationToken token : words) {
            if (!EnglishLexiconProfiles.isClosedClass(token.getNormalized())
                    && !EnglishLexiconProfiles.DISCOURSE_FILLERS.contains(token.getNormalized())) {
                contentCount++;
            }
        }
        double contentRatio = words.isEmpty() ? 0.0 : (double) contentCount / words.size();
        if (mode == DictationMode.EASY) {
            return contentRatio >= 0.55 ? range.max() : range.min();
        }
        if (mode == DictationMode.MEDIUM) {
            return (int) Math.round(range.min() + (range.max() - range.min()) * Math.min(1.0, contentRatio / 0.65));
        }
        return range.min() + (int) Math.round((range.max() - range.min()) * 0.65);
    }

    private List<Integer> greedySelect(
            List<WordAnalysis> analyses,
            Map<Integer, Double> baseScores,
            int targetCount,
            int maxConsecutive,
            DictationMode mode,
            int wordCount) {
        Set<Integer> selected = new HashSet<>();
        Map<String, Integer> selectedWordCounts = new HashMap<>();

        int maxBlanksByVisibility = wordCount >= 5
                ? (int) Math.floor(wordCount * (1.0 - DictationBlankConfig.MIN_VISIBLE_WORD_RATIO))
                : Math.max(1, wordCount - 1);
        int effectiveTarget = Math.min(targetCount, maxBlanksByVisibility);

        for (int step = 0; step < effectiveTarget; step++) {
            WordAnalysis bestCandidate = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (WordAnalysis candidate : analyses) {
                int idx = candidate.wordIndex();
                if (selected.contains(idx)) {
                    continue;
                }
                if (mode == DictationMode.EASY && candidate.strictlyExcludedInEasy()
                        && hasAnyNonExcludedRemaining(analyses, selected, true)) {
                    continue;
                }
                if (exceedsConsecutiveLimit(selected, idx, maxConsecutive)) {
                    continue;
                }
                if ((mode == DictationMode.EASY || mode == DictationMode.MEDIUM)
                        && wouldHideAuxAndAdjacentMainVerb(analyses, selected, idx)) {
                    continue;
                }
                if (mode == DictationMode.EASY && idx == 0 && wordCount >= 5
                        && hasOtherEligibleCandidate(analyses, selected, maxConsecutive, mode)) {
                    continue;
                }

                double score = baseScores.getOrDefault(idx, 0.0);
                int priorRepeats = selectedWordCounts.getOrDefault(candidate.normalized(), 0);
                if (priorRepeats > 0) {
                    score -= DictationBlankConfig.REPETITION_PENALTY * priorRepeats;
                }

                if (bestCandidate == null
                        || Double.compare(score, bestScore) > 0
                        || (Double.compare(score, bestScore) == 0 && idx < bestCandidate.wordIndex())) {
                    bestCandidate = candidate;
                    bestScore = score;
                }
            }

            if (bestCandidate == null) {
                break;
            }
            if (bestScore <= 0.0 && mode == DictationMode.EASY && !selected.isEmpty()) {
                break;
            }

            selected.add(bestCandidate.wordIndex());
            selectedWordCounts.merge(bestCandidate.normalized(), 1, Integer::sum);
        }

        List<Integer> ordered = new ArrayList<>(selected);
        ordered.sort(Comparator.naturalOrder());
        return ordered;
    }

    private boolean hasAnyNonExcludedRemaining(
            List<WordAnalysis> analyses,
            Set<Integer> selected,
            boolean checkEasyExclusion) {
        for (WordAnalysis analysis : analyses) {
            if (!selected.contains(analysis.wordIndex())) {
                if (!checkEasyExclusion || !analysis.strictlyExcludedInEasy()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasOtherEligibleCandidate(
            List<WordAnalysis> analyses,
            Set<Integer> selected,
            int maxConsecutive,
            DictationMode mode) {
        for (WordAnalysis analysis : analyses) {
            int idx = analysis.wordIndex();
            if (idx == 0 || selected.contains(idx)) {
                continue;
            }
            if (mode == DictationMode.EASY && analysis.strictlyExcludedInEasy()) {
                continue;
            }
            if (!exceedsConsecutiveLimit(selected, idx, maxConsecutive)
                    && !wouldHideAuxAndAdjacentMainVerb(analyses, selected, idx)) {
                return true;
            }
        }
        return false;
    }

    private boolean exceedsConsecutiveLimit(Set<Integer> selected, int candidateIdx, int maxConsecutive) {
        int left = 0;
        int cursor = candidateIdx - 1;
        while (selected.contains(cursor)) {
            left++;
            cursor--;
        }
        int right = 0;
        cursor = candidateIdx + 1;
        while (selected.contains(cursor)) {
            right++;
            cursor++;
        }
        return (left + 1 + right) > maxConsecutive;
    }

    private boolean wouldHideAuxAndAdjacentMainVerb(
            List<WordAnalysis> analyses,
            Set<Integer> selected,
            int candidateIdx) {
        WordAnalysis current = analyses.get(candidateIdx);
        if (current.auxiliary() && candidateIdx + 1 < analyses.size()) {
            WordAnalysis next = analyses.get(candidateIdx + 1);
            if (selected.contains(next.wordIndex()) && next.contentVerb()) {
                return true;
            }
        }
        if (current.contentVerb() && candidateIdx - 1 >= 0) {
            WordAnalysis prev = analyses.get(candidateIdx - 1);
            if (selected.contains(prev.wordIndex()) && prev.auxiliary()) {
                return true;
            }
        }
        return false;
    }

    private List<WordAnalysis> analyzeWords(List<DictationToken> words, Map<String, LexicalWordInfo> lexMap) {
        List<WordAnalysis> analyses = new ArrayList<>(words.size());
        for (int i = 0; i < words.size(); i++) {
            DictationToken token = words.get(i);
            String norm = token.getNormalized();
            String prevNorm = i > 0 ? words.get(i - 1).getNormalized() : null;
            String nextNorm = i + 1 < words.size() ? words.get(i + 1).getNormalized() : null;
            LexicalWordInfo info = lexMap.get(norm);

            boolean negativeContraction = EnglishLexiconProfiles.isNegativeContraction(norm);
            boolean contraction = EnglishLexiconProfiles.isContraction(norm);
            boolean negator = EnglishLexiconProfiles.NEGATORS.contains(norm) || negativeContraction;
            boolean number = EnglishLexiconProfiles.isNumberToken(norm);
            boolean filler = EnglishLexiconProfiles.DISCOURSE_FILLERS.contains(norm)
                    && !(i > 0 && "well".equals(norm) && isModifierOrAdverbContext(prevNorm));
            boolean auxiliary = EnglishLexiconProfiles.AUXILIARIES_MODALS.contains(norm);
            boolean closedClass = EnglishLexiconProfiles.isClosedClass(norm) && !negator;

            boolean phrasalParticle = EnglishLexiconProfiles.PHRASAL_PARTICLES.contains(norm)
                    && prevNorm != null
                    && isLikelyPhrasalVerbHead(prevNorm, lexMap.get(prevNorm));

            boolean phrasalVerbHead = nextNorm != null
                    && EnglishLexiconProfiles.PHRASAL_PARTICLES.contains(nextNorm)
                    && isLikelyPhrasalVerbHead(norm, info);

            boolean afterDeterminer = prevNorm != null && EnglishLexiconProfiles.ARTICLES_DETERMINERS.contains(prevNorm);
            boolean afterModalOrAux = prevNorm != null && (EnglishLexiconProfiles.AUXILIARIES_MODALS.contains(prevNorm)
                    || "to".equals(prevNorm));

            PosCategory posCategory = classifyPos(
                    norm, info, closedClass, negator, number, contraction, phrasalParticle, afterDeterminer, afterModalOrAux);

            boolean contentVerb = posCategory == PosCategory.VERB || phrasalVerbHead;
            boolean strictlyExcludedInEasy = closedClass && !phrasalParticle && !contraction && !negator && !number;

            boolean cvLinking = nextNorm != null
                    && EnglishLexiconProfiles.endsWithConsonantChar(norm)
                    && EnglishLexiconProfiles.startsWithVowelSound(nextNorm);

            boolean hDropping = EnglishLexiconProfiles.H_DROPPING_WORDS.contains(norm) && i > 0;
            boolean edOrSElision = (norm.endsWith("ed") || norm.endsWith("s"))
                    && norm.length() > 3
                    && !closedClass
                    && nextNorm != null
                    && EnglishLexiconProfiles.endsWithConsonantChar(nextNorm.substring(0, 1));

            analyses.add(new WordAnalysis(
                    token.getWordIndex(),
                    token.getText(),
                    norm,
                    posCategory,
                    closedClass,
                    auxiliary,
                    contentVerb,
                    negator,
                    number,
                    filler,
                    contraction,
                    phrasalParticle,
                    phrasalVerbHead,
                    strictlyExcludedInEasy,
                    cvLinking,
                    hDropping,
                    edOrSElision,
                    info));
        }
        return analyses;
    }

    private boolean isModifierOrAdverbContext(String prevNorm) {
        return prevNorm != null && Set.of("very", "pretty", "quite", "so", "as", "too", "doing", "went", "sleep").contains(prevNorm);
    }

    private boolean isLikelyPhrasalVerbHead(String word, LexicalWordInfo info) {
        if (EnglishLexiconProfiles.isClosedClass(word) || EnglishLexiconProfiles.NUMBER_WORDS.contains(word)) {
            return false;
        }
        if (info != null && info.hasPos("verb")) {
            return true;
        }
        return Set.of(
                "give", "gave", "given", "take", "took", "taken", "put", "puts", "set", "sets",
                "get", "got", "gotten", "turn", "turned", "look", "looked", "bring", "brought",
                "come", "came", "go", "went", "gone", "pick", "picked", "find", "found",
                "work", "worked", "figure", "figured", "carry", "carried", "keep", "kept",
                "break", "broke", "broken", "call", "called", "run", "ran", "check", "checked",
                "wake", "woke", "stand", "stood", "make", "made", "hold", "held", "show", "showed",
                "slow", "slowed", "speed", "sped", "clean", "cleaned", "settle", "settled")
                .contains(word);
    }

    private PosCategory classifyPos(
            String norm,
            LexicalWordInfo info,
            boolean closedClass,
            boolean negator,
            boolean number,
            boolean contraction,
            boolean phrasalParticle,
            boolean afterDeterminer,
            boolean afterModalOrAux) {
        if (phrasalParticle) {
            return PosCategory.PHRASAL_PARTICLE;
        }
        if (negator) {
            return PosCategory.NEGATOR;
        }
        if (number) {
            return PosCategory.NUMBER;
        }
        if (contraction) {
            return PosCategory.CONTRACTION;
        }
        if (closedClass) {
            return PosCategory.FUNCTION_WORD;
        }

        if (info != null && !info.getPosTags().isEmpty()) {
            if (afterDeterminer && info.hasPos("noun")) {
                return PosCategory.NOUN;
            }
            if (afterModalOrAux && info.hasPos("verb")) {
                return PosCategory.VERB;
            }
            if (info.hasPos("noun")) {
                return PosCategory.NOUN;
            }
            if (info.hasPos("verb")) {
                return PosCategory.VERB;
            }
            if (info.hasPos("adjective")) {
                return PosCategory.ADJECTIVE;
            }
            if (info.hasPos("adverb")) {
                return PosCategory.ADVERB;
            }
        }

        for (Map.Entry<String, String> entry : EnglishLexiconProfiles.SUFFIX_POS_HINTS.entrySet()) {
            if (norm.length() > entry.getKey().length() + 2 && norm.endsWith(entry.getKey())) {
                return switch (entry.getValue()) {
                    case "noun" -> PosCategory.NOUN;
                    case "verb" -> PosCategory.VERB;
                    case "adjective" -> PosCategory.ADJECTIVE;
                    case "adverb" -> PosCategory.ADVERB;
                    default -> PosCategory.CONTENT_OTHER;
                };
            }
        }
        if (afterModalOrAux) {
            return PosCategory.VERB;
        }
        if (afterDeterminer) {
            return PosCategory.NOUN;
        }
        return PosCategory.CONTENT_OTHER;
    }

    private double scoreToken(WordAnalysis a, DictationMode mode, int wordCount) {
        ModeWeights w = DictationBlankConfig.weightsFor(mode);

        double sPos = computeSPos(a, mode);
        double sSem = computeSSem(a);
        double sCefr = computeSCefr(a.lexInfo() != null ? a.lexInfo().getPrimaryCefr() : null, mode);
        double sListen = computeSListen(a);
        double sLearn = computeSLearn(a);

        double rawScore = w.wPos() * sPos
                + w.wSem() * sSem
                + w.wCefr() * sCefr
                + w.wListen() * sListen
                + w.wLearn() * sLearn;

        double modeMultiplier = computeModeMultiplier(a, mode);
        double score = rawScore * modeMultiplier;

        if (mode == DictationMode.EASY && a.wordIndex() == 0 && wordCount >= 5) {
            score *= 0.85;
        }
        if (a.filler()) {
            score *= 0.20;
        }
        return score;
    }

    /**
     * POS suitability signal in [0, 1].
     */
    double computeSPos(WordAnalysis a, DictationMode mode) {
        return switch (a.posCategory()) {
            case NOUN, VERB -> 1.0;
            case ADJECTIVE, ADVERB -> 0.85;
            case NUMBER -> 0.90;
            case NEGATOR -> 0.90;
            case PHRASAL_PARTICLE -> 0.80;
            case CONTRACTION -> mode == DictationMode.EASY ? 0.55 : 0.85;
            case CONTENT_OTHER -> 0.75;
            case FUNCTION_WORD -> switch (mode) {
                case EASY -> 0.0;
                case MEDIUM -> 0.25;
                case HARD -> 0.70;
                case FULL -> 0.0;
            };
        };
    }

    private double computeModeMultiplier(WordAnalysis a, DictationMode mode) {
        if (a.posCategory() == PosCategory.FUNCTION_WORD) {
            return switch (mode) {
                case EASY -> 0.0;
                case MEDIUM -> 0.25;
                case HARD -> 1.0;
                case FULL -> 0.0;
            };
        }
        if (a.posCategory() == PosCategory.PHRASAL_PARTICLE || a.posCategory() == PosCategory.CONTRACTION) {
            return switch (mode) {
                case EASY -> 0.70;
                case MEDIUM -> 0.85;
                case HARD -> 1.0;
                case FULL -> 0.0;
            };
        }
        return 1.0;
    }

    /**
     * Semantic / communicative salience signal in [0, 1].
     */
    double computeSSem(WordAnalysis a) {
        if (a.filler()) {
            return 0.05;
        }
        double score = switch (a.posCategory()) {
            case NOUN, VERB, NUMBER -> 0.90;
            case NEGATOR -> 0.95;
            case ADJECTIVE, ADVERB -> 0.80;
            case PHRASAL_PARTICLE -> 0.85;
            case CONTRACTION -> a.negator() ? 0.95 : 0.55;
            case CONTENT_OTHER -> 0.70;
            case FUNCTION_WORD -> 0.20;
        };
        if (a.phrasalVerbHead()) {
            score = Math.min(1.0, score + 0.10);
        }
        return score;
    }

    /**
     * Target-band CEFR fit curve in [0, 1].
     * EASY peaks at A2-B1 (not monotonically increasing), MEDIUM peaks at B1-B2, HARD peaks at B2-C2.
     */
    public static double computeSCefr(String cefrLevel, DictationMode mode) {
        String level = cefrLevel == null ? "B1" : cefrLevel.trim().toUpperCase(Locale.ROOT);
        return switch (mode) {
            case EASY -> switch (level) {
                case "A1" -> 0.65;
                case "A2" -> 0.95;
                case "B1" -> 1.00;
                case "B2" -> 0.65;
                case "C1" -> 0.30;
                case "C2" -> 0.20;
                default -> 0.60;
            };
            case MEDIUM -> switch (level) {
                case "A1" -> 0.35;
                case "A2" -> 0.65;
                case "B1" -> 0.95;
                case "B2" -> 1.00;
                case "C1" -> 0.75;
                case "C2" -> 0.50;
                default -> 0.70;
            };
            case HARD -> switch (level) {
                case "A1" -> 0.20;
                case "A2" -> 0.35;
                case "B1" -> 0.65;
                case "B2" -> 0.90;
                case "C1" -> 1.00;
                case "C2" -> 1.00;
                default -> 0.70;
            };
            case FULL -> 0.0;
        };
    }

    /**
     * Acoustic & listening perception challenge signal in [0, 1].
     * Driven by weak forms, contractions, connected speech, elision, homophones, and silent-letter / IPA mismatch,
     * NOT raw character length.
     */
    double computeSListen(WordAnalysis a) {
        double score = 0.20;
        String norm = a.normalized();
        if (EnglishLexiconProfiles.WEAK_FORM_WORDS.contains(norm)) {
            score += 0.30;
        }
        if (a.contraction()) {
            score += 0.30;
        }
        if (EnglishLexiconProfiles.HOMOPHONES.contains(norm)) {
            score += 0.35;
        }
        if (a.cvLinking()) {
            score += 0.15;
        }
        if (a.hDropping()) {
            score += 0.20;
        }
        if (a.edOrSElision()) {
            score += 0.20;
        }
        if (EnglishLexiconProfiles.hasSilentLetterOrOpaqueOrthography(norm)) {
            score += 0.30;
        }
        if (a.lexInfo() != null && hasIpaSpellingMismatch(norm, a.lexInfo().getIpa())) {
            score += 0.20;
        }
        return Math.min(1.0, score);
    }

    private boolean hasIpaSpellingMismatch(String word, String ipa) {
        if (ipa == null || ipa.isBlank() || word == null || word.isBlank()) {
            return false;
        }
        String cleanIpa = ipa.replaceAll("[/\\[\\]ˈˌ.\\s]", "");
        return !cleanIpa.isEmpty() && word.length() - cleanIpa.length() >= 3;
    }

    /**
     * Pedagogical / learning value signal in [0, 1].
     */
    double computeSLearn(WordAnalysis a) {
        double score = 0.30;
        if (a.lexInfo() != null && a.lexInfo().isInVocab()) {
            score += 0.35;
            if (a.lexInfo().isHasCollocations()) {
                score += 0.15;
            }
            if (a.lexInfo().isInflectedForm()) {
                score += 0.15;
            }
        } else if (a.lexInfo() != null && a.lexInfo().isInflectedForm()) {
            score += 0.15;
        }
        if (a.phrasalVerbHead() || a.phrasalParticle()) {
            score += 0.20;
        }
        return Math.min(1.0, score);
    }

    enum PosCategory {
        NOUN,
        VERB,
        ADJECTIVE,
        ADVERB,
        NUMBER,
        NEGATOR,
        PHRASAL_PARTICLE,
        CONTRACTION,
        CONTENT_OTHER,
        FUNCTION_WORD
    }

    record WordAnalysis(
            int wordIndex,
            String text,
            String normalized,
            PosCategory posCategory,
            boolean closedClass,
            boolean auxiliary,
            boolean contentVerb,
            boolean negator,
            boolean number,
            boolean filler,
            boolean contraction,
            boolean phrasalParticle,
            boolean phrasalVerbHead,
            boolean strictlyExcludedInEasy,
            boolean cvLinking,
            boolean hDropping,
            boolean edOrSElision,
            LexicalWordInfo lexInfo) {
    }
}
