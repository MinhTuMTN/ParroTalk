package com.parrotalk.backend.service;

import com.parrotalk.backend.dto.dictionary.ContextualDictionaryLookupRequest;
import com.parrotalk.backend.dto.dictionary.ContextualDictionaryLookupResponse;
import com.parrotalk.backend.entity.DictionaryContextLookup;
import com.parrotalk.backend.repository.DictionaryContextLookupRepository;
import com.parrotalk.backend.util.ContextHashGenerator;
import com.parrotalk.backend.util.WordNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContextualDictionaryService {

    private static final String FALLBACK_SOURCE = "fallback";

    private final DictionaryContextLookupRepository contextLookupRepository;
    private final WordNormalizer wordNormalizer;
    private final ContextHashGenerator contextHashGenerator;

    @Transactional(readOnly = true)
    public ContextualDictionaryLookupResponse lookup(ContextualDictionaryLookupRequest request) {
        String normalizedWord = wordNormalizer.normalize(request.word());
        if (normalizedWord.isBlank()) {
            throw new IllegalArgumentException("word must not be blank");
        }
        if (request.contextText() == null || request.contextText().isBlank()) {
            throw new IllegalArgumentException("contextText must not be blank");
        }

        String contextHash = contextHashGenerator.generate(normalizedWord, request.contextText());

        return contextLookupRepository.findByNormalizedWordAndContextHash(normalizedWord, contextHash)
                .map(lookup -> mapCached(lookup, request.word()))
                .orElseGet(() -> fallback(request.word(), normalizedWord, contextHash));
    }

    private ContextualDictionaryLookupResponse mapCached(DictionaryContextLookup lookup, String word) {
        return new ContextualDictionaryLookupResponse(
                lookup.getId(),
                word,
                lookup.getNormalizedWord(),
                lookup.getContextHash(),
                lookup.getMeaningVi(),
                lookup.getShortMeaningVi(),
                lookup.getExplanationVi(),
                lookup.getPartOfSpeech(),
                lookup.getConfidence(),
                lookup.getProvider(),
                true
        );
    }

    private ContextualDictionaryLookupResponse fallback(String word, String normalizedWord, String contextHash) {
        return new ContextualDictionaryLookupResponse(
                null,
                word,
                normalizedWord,
                contextHash,
                null,
                null,
                null,
                null,
                null,
                FALLBACK_SOURCE,
                false
        );
    }
}
