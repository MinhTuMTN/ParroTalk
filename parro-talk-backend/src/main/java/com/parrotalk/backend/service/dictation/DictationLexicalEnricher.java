package com.parrotalk.backend.service.dictation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularyPronunciation;
import com.parrotalk.backend.entity.VocabularySense;
import com.parrotalk.backend.repository.VocabularyRepository;
import com.parrotalk.backend.service.dictation.DictationTokenizationService.DictationToken;
import com.parrotalk.backend.util.WordFormGenerator;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * Batch-enriches dictation tokens from the local {@code vocabularies} table (and {@link WordFormGenerator}
 * lemma candidates) with POS tags, CEFR levels, IPA pronunciations, collocation counts, and inflected flags.
 * Zero runtime AI calls.
 */
@Service
@RequiredArgsConstructor
public class DictationLexicalEnricher {

    private final VocabularyRepository vocabularyRepository;
    private final WordFormGenerator wordFormGenerator;

    /**
     * Enriches all word tokens across one or more segments in a single batch repository call.
     */
    @Transactional(readOnly = true)
    public Map<String, LexicalWordInfo> enrichTokens(Collection<DictationToken> wordTokens) {
        if (wordTokens == null || wordTokens.isEmpty()) {
            return Map.of();
        }

        Set<String> normalizedWords = new LinkedHashSet<>();
        for (DictationToken token : wordTokens) {
            if (token != null && token.isWord() && !token.getNormalized().isEmpty()) {
                normalizedWords.add(token.getNormalized());
            }
        }
        if (normalizedWords.isEmpty()) {
            return Map.of();
        }

        Map<String, Set<String>> candidateToTokens = new HashMap<>();
        Set<String> lookupKeys = new LinkedHashSet<>();
        for (String word : normalizedWords) {
            lookupKeys.add(word);
            candidateToTokens.computeIfAbsent(word, k -> new HashSet<>()).add(word);
            for (String lemma : inferLemmaCandidates(word)) {
                lookupKeys.add(lemma);
                candidateToTokens.computeIfAbsent(lemma, k -> new HashSet<>()).add(word);
            }
        }

        List<Vocabulary> matchedVocabularies = lookupKeys.isEmpty()
                ? List.of()
                : vocabularyRepository.findByNormalizedWordInWithSenses(lookupKeys);

        Map<String, Vocabulary> exactByWord = new HashMap<>();
        Map<String, Vocabulary> lemmaByToken = new HashMap<>();

        for (Vocabulary vocab : matchedVocabularies) {
            if (vocab == null || vocab.getNormalizedWord() == null) {
                continue;
            }
            String vocabWord = vocab.getNormalizedWord().toLowerCase(Locale.ROOT);
            if (normalizedWords.contains(vocabWord)) {
                exactByWord.putIfAbsent(vocabWord, vocab);
            }
            Set<String> mappedTokens = candidateToTokens.getOrDefault(vocabWord, Set.of());
            for (String surfaceToken : mappedTokens) {
                if (!surfaceToken.equals(vocabWord) && matchesInflectedForm(vocab, vocabWord, surfaceToken)) {
                    lemmaByToken.putIfAbsent(surfaceToken, vocab);
                }
            }
        }

        Map<String, LexicalWordInfo> result = new HashMap<>();
        for (String word : normalizedWords) {
            Vocabulary exact = exactByWord.get(word);
            Vocabulary lemmaVocab = lemmaByToken.get(word);
            Vocabulary resolved = exact != null ? exact : lemmaVocab;

            if (resolved == null) {
                result.put(word, LexicalWordInfo.builder()
                        .normalizedWord(word)
                        .inVocab(false)
                        .inflectedForm(isObviouslyInflected(word))
                        .posTags(Set.of())
                        .cefrLevels(Set.of())
                        .hasCollocations(false)
                        .build());
                continue;
            }

            Set<String> posTags = new LinkedHashSet<>();
            Set<String> cefrLevels = new LinkedHashSet<>();
            boolean hasCollocations = false;
            if (resolved.getSenses() != null) {
                for (VocabularySense sense : resolved.getSenses()) {
                    if (sense.getPartOfSpeech() != null && !sense.getPartOfSpeech().isBlank()) {
                        posTags.add(sense.getPartOfSpeech().trim().toLowerCase(Locale.ROOT));
                    }
                    if (sense.getCefrLevel() != null && !sense.getCefrLevel().isBlank()) {
                        cefrLevels.add(sense.getCefrLevel().trim().toUpperCase(Locale.ROOT));
                    }
                    if (sense.getCollocations() != null && !sense.getCollocations().isEmpty()) {
                        hasCollocations = true;
                    }
                }
            }

            String ipa = null;
            if (resolved.getPronunciations() != null) {
                for (VocabularyPronunciation pron : resolved.getPronunciations()) {
                    if (pron.getIpa() != null && !pron.getIpa().isBlank()) {
                        ipa = pron.getIpa().trim();
                        break;
                    }
                }
            }

            boolean inflected = exact == null || isObviouslyInflected(word);
            result.put(word, LexicalWordInfo.builder()
                    .normalizedWord(word)
                    .inVocab(true)
                    .inflectedForm(inflected)
                    .posTags(Set.copyOf(posTags))
                    .cefrLevels(Set.copyOf(cefrLevels))
                    .primaryCefr(pickRepresentativeCefr(cefrLevels))
                    .ipa(ipa)
                    .hasCollocations(hasCollocations)
                    .build());
        }

        return result;
    }

    private boolean matchesInflectedForm(Vocabulary vocab, String baseWord, String surfaceToken) {
        if (vocab.getWordForms() != null && vocab.getWordForms().contains(surfaceToken)) {
            return true;
        }
        return wordFormGenerator.generateForms(baseWord).contains(surfaceToken);
    }

    Set<String> inferLemmaCandidates(String word) {
        Set<String> candidates = new LinkedHashSet<>();
        if (word == null || word.length() < 3) {
            return candidates;
        }
        if (word.endsWith("ies") && word.length() > 4) {
            candidates.add(word.substring(0, word.length() - 3) + "y");
        }
        if (word.endsWith("ves") && word.length() > 4) {
            candidates.add(word.substring(0, word.length() - 3) + "f");
            candidates.add(word.substring(0, word.length() - 3) + "fe");
        }
        if (word.endsWith("es") && word.length() > 3) {
            candidates.add(word.substring(0, word.length() - 2));
        }
        if (word.endsWith("s") && word.length() > 3) {
            candidates.add(word.substring(0, word.length() - 1));
        }
        if (word.endsWith("ied") && word.length() > 4) {
            candidates.add(word.substring(0, word.length() - 3) + "y");
        }
        if (word.endsWith("ed") && word.length() > 3) {
            String stem = word.substring(0, word.length() - 2);
            candidates.add(stem);
            candidates.add(stem + "e");
            if (stem.length() >= 2 && stem.charAt(stem.length() - 1) == stem.charAt(stem.length() - 2)) {
                candidates.add(stem.substring(0, stem.length() - 1));
            }
        }
        if (word.endsWith("ing") && word.length() > 4) {
            String stem = word.substring(0, word.length() - 3);
            candidates.add(stem);
            candidates.add(stem + "e");
            if (stem.length() >= 2 && stem.charAt(stem.length() - 1) == stem.charAt(stem.length() - 2)) {
                candidates.add(stem.substring(0, stem.length() - 1));
            }
        }
        if (word.endsWith("er") && word.length() > 3) {
            String stem = word.substring(0, word.length() - 2);
            candidates.add(stem);
            candidates.add(stem + "e");
        }
        if (word.endsWith("est") && word.length() > 4) {
            String stem = word.substring(0, word.length() - 3);
            candidates.add(stem);
            candidates.add(stem + "e");
        }
        return candidates;
    }

    private boolean isObviouslyInflected(String word) {
        if (word == null || word.length() < 3) {
            return false;
        }
        if (EnglishLexiconProfiles.IRREGULAR_INFLECTIONS.contains(word)) {
            return true;
        }
        if (EnglishLexiconProfiles.isClosedClass(word) || EnglishLexiconProfiles.isContraction(word)) {
            return false;
        }
        return word.endsWith("ed")
                || word.endsWith("ing")
                || (word.endsWith("ies") && word.length() > 4)
                || (word.endsWith("s") && word.length() > 4 && !word.endsWith("ss") && !word.endsWith("ous") && !word.endsWith("is"));
    }

    private String pickRepresentativeCefr(Set<String> cefrLevels) {
        for (String level : List.of("A1", "A2", "B1", "B2", "C1", "C2")) {
            if (cefrLevels.contains(level)) {
                return level;
            }
        }
        return null;
    }

    @Value
    @Builder
    public static class LexicalWordInfo {
        String normalizedWord;
        boolean inVocab;
        boolean inflectedForm;
        Set<String> posTags;
        Set<String> cefrLevels;
        String primaryCefr;
        String ipa;
        boolean hasCollocations;

        public boolean hasPos(String posPrefix) {
            for (String tag : posTags) {
                if (tag.contains(posPrefix)) {
                    return true;
                }
            }
            return false;
        }
    }
}
