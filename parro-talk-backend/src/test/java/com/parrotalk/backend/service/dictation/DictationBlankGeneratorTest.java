package com.parrotalk.backend.service.dictation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.parrotalk.backend.constant.DictationMode;
import com.parrotalk.backend.dto.dictation.BlankSlotDto;
import com.parrotalk.backend.dto.dictation.SegmentBlanksPayload;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularyPronunciation;
import com.parrotalk.backend.entity.VocabularySense;
import com.parrotalk.backend.repository.VocabularyRepository;
import com.parrotalk.backend.service.dictation.DictationBlankGenerator.PosCategory;
import com.parrotalk.backend.service.dictation.DictationBlankGenerator.WordAnalysis;
import com.parrotalk.backend.service.dictation.DictationLexicalEnricher.LexicalWordInfo;
import com.parrotalk.backend.service.dictation.DictationTokenizationService.DictationToken;
import com.parrotalk.backend.util.WordFormGenerator;

class DictationBlankGeneratorTest {

    private VocabularyRepository vocabularyRepository;
    private DictationTokenizationService tokenizationService;
    private DictationBlankGenerator generator;

    @BeforeEach
    void setUp() {
        vocabularyRepository = mock(VocabularyRepository.class);
        when(vocabularyRepository.findByNormalizedWordInWithSenses(anyCollection())).thenReturn(List.of());
        tokenizationService = new DictationTokenizationService();
        DictationLexicalEnricher enricher = new DictationLexicalEnricher(vocabularyRepository, new WordFormGenerator());
        generator = new DictationBlankGenerator(tokenizationService, enricher);
    }

    @Test
    void unsuitableAndVeryShortSentencesHandledCleanly() {
        for (String unsuitable : List.of("[Music]", "(Applause)", "...", "Uh.", "Uh oh!")) {
            SegmentBlanksPayload payload = generator.generateForSegment(unsuitable);
            assertFalse(payload.isSuitable(), "Expected unsuitable for: " + unsuitable);
            assertTrue(payload.getLevels().get(DictationMode.EASY).isEmpty());
            assertTrue(payload.getLevels().get(DictationMode.MEDIUM).isEmpty());
            assertTrue(payload.getLevels().get(DictationMode.HARD).isEmpty());
        }

        // 2-4 words sentence -> suitable = true, Easy 1, Medium 1, Hard min(N-1, 2)
        SegmentBlanksPayload short3 = generator.generateForSegment("Coffee tastes amazing.");
        assertTrue(short3.isSuitable());
        assertEquals(3, short3.getWordCount());
        assertEquals(1, short3.getLevels().get(DictationMode.EASY).size());
        assertEquals(1, short3.getLevels().get(DictationMode.MEDIUM).size());
        assertEquals(2, short3.getLevels().get(DictationMode.HARD).size());
    }

    @Test
    void respectsBlankCountsVisibilityAndConsecutiveConstraintsAcrossLengths() {
        String s6 = "Students discuss complex ideas every morning.";
        SegmentBlanksPayload p6 = generator.generateForSegment(s6);
        assertEquals(6, p6.getWordCount());
        assertEquals(1, p6.getLevels().get(DictationMode.EASY).size());
        assertEquals(2, p6.getLevels().get(DictationMode.MEDIUM).size());
        int hard6 = p6.getLevels().get(DictationMode.HARD).size();
        assertTrue(hard6 >= 3 && hard6 <= 4);
        assertValidStructuralConstraints(s6, p6);

        String s10 = "My colleague actually brought fifteen fresh notebooks to the office.";
        SegmentBlanksPayload p10 = generator.generateForSegment(s10);
        assertEquals(10, p10.getWordCount());
        assertRange(p10, DictationMode.EASY, 1, 2);
        assertRange(p10, DictationMode.MEDIUM, 3, 4);
        assertRange(p10, DictationMode.HARD, 5, 6); // 40% visible out of 10 = max 6 blanks
        assertValidStructuralConstraints(s10, p10);

        String s15 = "Before starting the afternoon presentation, our manager carefully reviewed every financial chart with the team.";
        SegmentBlanksPayload p15 = generator.generateForSegment(s15);
        assertEquals(15, p15.getWordCount());
        assertRange(p15, DictationMode.EASY, 2, 3);
        assertRange(p15, DictationMode.MEDIUM, 4, 6);
        assertRange(p15, DictationMode.HARD, 7, 9); // floor(15*0.6)=9
        assertValidStructuralConstraints(s15, p15);

        String s22 = "Although the unexpected storm delayed several international flights this morning, passengers remained remarkably patient while airline staff arranged comfortable hotel accommodations.";
        SegmentBlanksPayload p22 = generator.generateForSegment(s22);
        assertTrue(p22.getWordCount() >= 19);
        assertRange(p22, DictationMode.EASY, 3, 4);
        assertRange(p22, DictationMode.MEDIUM, 6, 8);
        assertRange(p22, DictationMode.HARD, 9, 12);
        assertValidStructuralConstraints(s22, p22);
    }

    @Test
    void isCompletelyDeterministicAndNeverHidesWordZeroInEasyWhenAlternativesExist() {
        String text = "Engineers quickly designed a durable bridge across the wide river.";
        SegmentBlanksPayload first = generator.generateForSegment(text);
        SegmentBlanksPayload second = generator.generateForSegment(text);
        assertEquals(first, second);

        List<BlankSlotDto> easySlots = first.getLevels().get(DictationMode.EASY);
        assertFalse(easySlots.isEmpty());
        assertTrue(easySlots.stream().noneMatch(slot -> slot.getWordIndex() == 0),
                "EASY mode should avoid blanking wordIndex 0 when alternatives exist");
    }

    @Test
    void deprioritizesFunctionWordsInEasyWhileEligibleInHard() {
        String text = "She was walking to the station with her friend.";
        List<DictationToken> words = tokenizationService.extractWordTokens(text);
        SegmentBlanksPayload payload = generator.generateForSegment(text);

        Set<String> easySelected = new HashSet<>();
        for (BlankSlotDto slot : payload.getLevels().get(DictationMode.EASY)) {
            easySelected.add(words.get(slot.getWordIndex()).getNormalized());
        }
        assertFalse(easySelected.contains("to"));
        assertFalse(easySelected.contains("the"));
        assertFalse(easySelected.contains("with"));

        assertTrue(payload.getLevels().get(DictationMode.HARD).size() >= 5);
    }

    @Test
    void appliesRepetitionPenaltyAcrossRepeatedWords() {
        String text = "The team supported the team while the managers praised the team today.";
        List<DictationToken> words = tokenizationService.extractWordTokens(text);
        SegmentBlanksPayload payload = generator.generateForSegment(text);

        long easyTeamCount = payload.getLevels().get(DictationMode.EASY).stream()
                .map(slot -> words.get(slot.getWordIndex()).getNormalized())
                .filter("team"::equals)
                .count();
        assertTrue(easyTeamCount <= 1, "Repeated word 'team' should be penalized on repeat in EASY mode");
    }

    @Test
    void avoidsHidingBothAuxiliaryAndAdjacentMainVerbInEasyAndMedium() {
        String text = "They could finish the urgent project before Friday.";
        List<DictationToken> words = tokenizationService.extractWordTokens(text);
        // index 1 = "could" (aux), index 2 = "finish" (main verb)
        assertEquals("could", words.get(1).getNormalized());
        assertEquals("finish", words.get(2).getNormalized());

        SegmentBlanksPayload payload = generator.generateForSegment(text);
        for (DictationMode mode : List.of(DictationMode.EASY, DictationMode.MEDIUM)) {
            Set<Integer> idxs = new HashSet<>();
            payload.getLevels().get(mode).forEach(s -> idxs.add(s.getWordIndex()));
            assertFalse(idxs.contains(1) && idxs.contains(2),
                    "Should not hide both auxiliary 'could' and adjacent verb 'finish' in " + mode);
        }
    }

    @Test
    void cefrSignalUsesNonMonotonicTargetBandFitCurves() {
        // EASY peaks at A2/B1 and drops sharply for C1/C2
        double easyA1 = DictationBlankGenerator.computeSCefr("A1", DictationMode.EASY);
        double easyA2 = DictationBlankGenerator.computeSCefr("A2", DictationMode.EASY);
        double easyB1 = DictationBlankGenerator.computeSCefr("B1", DictationMode.EASY);
        double easyB2 = DictationBlankGenerator.computeSCefr("B2", DictationMode.EASY);
        double easyC1 = DictationBlankGenerator.computeSCefr("C1", DictationMode.EASY);
        double easyC2 = DictationBlankGenerator.computeSCefr("C2", DictationMode.EASY);
        assertTrue(easyB1 > easyA1);
        assertTrue(easyA2 > easyB2);
        assertTrue(easyB1 > easyC1);
        assertTrue(easyC1 > easyC2);

        // MEDIUM peaks at B1/B2
        double medA1 = DictationBlankGenerator.computeSCefr("A1", DictationMode.MEDIUM);
        double medB2 = DictationBlankGenerator.computeSCefr("B2", DictationMode.MEDIUM);
        double medC2 = DictationBlankGenerator.computeSCefr("C2", DictationMode.MEDIUM);
        assertTrue(medB2 > medA1);
        assertTrue(medB2 > medC2);

        // HARD increases into B2-C2
        double hardA2 = DictationBlankGenerator.computeSCefr("A2", DictationMode.HARD);
        double hardB2 = DictationBlankGenerator.computeSCefr("B2", DictationMode.HARD);
        double hardC1 = DictationBlankGenerator.computeSCefr("C1", DictationMode.HARD);
        assertTrue(hardC1 > hardB2);
        assertTrue(hardB2 > hardA2);
    }

    @Test
    void listeningSignalDrivenByPhonologyAndHomophonesRatherThanRawLength() {
        WordAnalysis longTransparent = new WordAnalysis(
                0, "understanding", "understanding", PosCategory.NOUN,
                false, false, false, false, false, false, false, false, false, false,
                false, false, false, null);
        WordAnalysis shortHomophone = new WordAnalysis(
                1, "they're", "they're", PosCategory.CONTRACTION,
                false, false, false, false, false, false, true, false, false, false,
                false, false, false, null);
        WordAnalysis opaqueSilent = new WordAnalysis(
                2, "knight", "knight", PosCategory.NOUN,
                false, false, false, false, false, false, false, false, false, false,
                false, false, false, LexicalWordInfo.builder()
                        .normalizedWord("knight")
                        .inVocab(true)
                        .ipa("/naɪt/")
                        .posTags(Set.of("noun"))
                        .cefrLevels(Set.of("B1"))
                        .build());

        double scoreLong = generator.computeSListen(longTransparent);
        double scoreHomophoneContraction = generator.computeSListen(shortHomophone);
        double scoreSilentIpa = generator.computeSListen(opaqueSilent);

        assertTrue(scoreHomophoneContraction > scoreLong);
        assertTrue(scoreSilentIpa > scoreLong);
    }

    @Test
    void usesVocabularyRepositoryEnrichmentAndHandlesContractionsAndPhrasalVerbs() {
        Vocabulary vocab = Vocabulary.builder()
                .id(UUID.randomUUID())
                .word("give")
                .normalizedWord("give")
                .senses(List.of(VocabularySense.builder().partOfSpeech("verb").cefrLevel("A2").build()))
                .pronunciations(List.of(VocabularyPronunciation.builder().ipa("/ɡɪv/").build()))
                .build();
        when(vocabularyRepository.findByNormalizedWordInWithSenses(anyCollection())).thenReturn(List.of(vocab));

        String text = "They didn't give up on their innovative plan.";
        List<DictationToken> words = tokenizationService.extractWordTokens(text);
        SegmentBlanksPayload payload = generator.generateForSegment(text);

        assertTrue(payload.isSuitable());
        assertNotEquals(payload.getLevels().get(DictationMode.EASY).size(),
                payload.getLevels().get(DictationMode.HARD).size());

        // Every selected slot wordIndex must map cleanly to a word token with matching charLength
        for (DictationMode mode : List.of(DictationMode.EASY, DictationMode.MEDIUM, DictationMode.HARD)) {
            for (BlankSlotDto slot : payload.getLevels().get(mode)) {
                DictationToken token = words.get(slot.getWordIndex());
                assertEquals(token.getText().length(), slot.getCharLength());
            }
        }
    }

    private void assertRange(SegmentBlanksPayload payload, DictationMode mode, int min, int max) {
        int count = payload.getLevels().get(mode).size();
        assertTrue(count >= min && count <= max,
                "Mode " + mode + " expected [" + min + "," + max + "] blanks but got " + count);
    }

    private void assertValidStructuralConstraints(String text, SegmentBlanksPayload payload) {
        List<DictationToken> words = tokenizationService.extractWordTokens(text);
        int wordCount = words.size();
        for (DictationMode mode : List.of(DictationMode.EASY, DictationMode.MEDIUM, DictationMode.HARD)) {
            List<BlankSlotDto> slots = payload.getLevels().get(mode);
            if (wordCount >= 5) {
                double visibleRatio = (double) (wordCount - slots.size()) / wordCount;
                assertTrue(visibleRatio >= 0.40 - 1e-9,
                        "Mode " + mode + " violated min visible ratio: " + visibleRatio);
            }
            int run = 0;
            int prev = -10;
            int maxRun = 0;
            for (BlankSlotDto slot : slots) {
                assertTrue(slot.getWordIndex() >= 0 && slot.getWordIndex() < wordCount);
                assertTrue(slot.getWordIndex() > prev, "Slots must be strictly sorted & unique");
                run = (slot.getWordIndex() == prev + 1) ? run + 1 : 1;
                maxRun = Math.max(maxRun, run);
                prev = slot.getWordIndex();
            }
            int allowedMaxRun = mode == DictationMode.EASY ? 1 : 2;
            assertTrue(maxRun <= allowedMaxRun,
                    "Mode " + mode + " consecutive blanks " + maxRun + " exceeded " + allowedMaxRun);
        }
    }
}
