package com.parrotalk.backend.service;

import com.parrotalk.backend.dto.dictionary.DictionaryLookupResponse;
import com.parrotalk.backend.entity.DictionaryEntry;
import com.parrotalk.backend.repository.DictionaryEntryRepository;
import com.parrotalk.backend.util.WordNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DictionaryLookupService {

    private static final String DEFAULT_LANGUAGE = "en";
    private static final String FALLBACK_SOURCE = "fallback";

    private final DictionaryEntryRepository dictionaryEntryRepository;
    private final WordNormalizer wordNormalizer;

    @Transactional
    public DictionaryLookupResponse lookup(String word) {
        String normalizedWord = wordNormalizer.normalize(word);
        if (normalizedWord.isBlank()) {
            throw new IllegalArgumentException("word must not be blank");
        }

        return dictionaryEntryRepository.findByNormalizedWordAndLanguage(normalizedWord, DEFAULT_LANGUAGE)
                .map(this::touchAndMap)
                .orElseGet(() -> fallback(word, normalizedWord));
    }

    private DictionaryLookupResponse touchAndMap(DictionaryEntry entry) {
        entry.setLastAccessedAt(LocalDateTime.now());
        DictionaryEntry saved = dictionaryEntryRepository.save(entry);
        return new DictionaryLookupResponse(
                saved.getId(),
                saved.getDisplayWord(),
                saved.getNormalizedWord(),
                saved.getLanguage(),
                saved.getPartOfSpeech(),
                saved.getPhonetic(),
                saved.getDefinitionsJson(),
                saved.getExamplesJson(),
                saved.getSynonymsJson(),
                saved.getAntonymsJson(),
                saved.getSource(),
                saved.getCefrLevel(),
                saved.getAudioUkUrl(),
                saved.getAudioUsUrl(),
                saved.getCommonMeaningVi(),
                true
        );
    }

    private DictionaryLookupResponse fallback(String word, String normalizedWord) {
        return new DictionaryLookupResponse(
                null,
                word,
                normalizedWord,
                DEFAULT_LANGUAGE,
                null,
                null,
                null,
                null,
                null,
                null,
                FALLBACK_SOURCE,
                null,
                null,
                null,
                null,
                false
        );
    }
}
