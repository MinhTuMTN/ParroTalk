package com.parrotalk.backend.service;

import com.parrotalk.backend.dto.vocabulary.VocabularyDetailDTO;
import com.parrotalk.backend.entity.Topic;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.repository.VocabularyRepository;
import com.parrotalk.backend.util.WordNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VocabularyService {

    private final VocabularyRepository vocabularyRepository;
    private WritingTtsService writingTtsService;
    private WordNormalizer wordNormalizer = new WordNormalizer();

    @Autowired(required = false)
    void setWritingTtsService(WritingTtsService writingTtsService) {
        this.writingTtsService = writingTtsService;
    }

    @Autowired(required = false)
    void setWordNormalizer(WordNormalizer wordNormalizer) {
        if (wordNormalizer != null) {
            this.wordNormalizer = wordNormalizer;
        }
    }

    private String resolveAudioUrl(String word, String accent, String storedAudioUrl) {
        if (writingTtsService != null) {
            return writingTtsService.resolvePronunciationAudioUrl(word, accent, storedAudioUrl);
        }
        return WritingTtsService.isBlockedExternalAudioUrl(storedAudioUrl) ? null : storedAudioUrl;
    }

    @Transactional(readOnly = true)
    public VocabularyDetailDTO getVocabularyByWord(String word) {
        String normalized = wordNormalizer.normalize(word);
        if (normalized.isEmpty()) {
            return null;
        }

        Optional<Vocabulary> exact = vocabularyRepository.findByNormalizedWord(normalized);
        if (exact.isPresent()) {
            return mapToDTO(exact.get(), null);
        }

        if (normalized.endsWith("'s") && normalized.length() > 2) {
            String stripped = normalized.substring(0, normalized.length() - 2);
            Optional<Vocabulary> strippedExact = vocabularyRepository.findByNormalizedWord(stripped);
            if (strippedExact.isPresent()) {
                return mapToDTO(strippedExact.get(), null);
            }
            Optional<Vocabulary> formMatch = vocabularyRepository.findFirstByWordForm(normalized);
            if (formMatch.isPresent()) {
                return mapToDTO(formMatch.get(), normalized);
            }
            Optional<Vocabulary> strippedFormMatch = vocabularyRepository.findFirstByWordForm(stripped);
            if (strippedFormMatch.isPresent()) {
                return mapToDTO(strippedFormMatch.get(), stripped);
            }
            return null;
        }

        Optional<Vocabulary> formMatch = vocabularyRepository.findFirstByWordForm(normalized);
        return formMatch.map(vocabulary -> mapToDTO(vocabulary, normalized)).orElse(null);
    }

    private VocabularyDetailDTO mapToDTO(Vocabulary entity, String matchedFrom) {
        return VocabularyDetailDTO.builder()
                .word(entity.getWord())
                .matchedFrom(matchedFrom)
                .commonMeaningVn(entity.getCommonMeaningVn())
                .topics(entity.getSenses().stream()
                        .map(s -> s.getTopic())
                        .filter(java.util.Objects::nonNull)
                        .map(Topic::getName).distinct()
                        .collect(Collectors.toList()))
                .pronunciations(entity.getPronunciations().stream()
                        .map(p -> VocabularyDetailDTO.PronunciationDTO.builder()
                                .accent(p.getAccent())
                                .ipa(p.getIpa())
                                .audioUrl(resolveAudioUrl(entity.getWord(), p.getAccent(), p.getAudioUrl()))
                                .build())
                        .collect(Collectors.toList()))
                .senses(entity.getSenses().stream()
                        .map(s -> VocabularyDetailDTO.SenseDTO.builder()
                                .partOfSpeech(s.getPartOfSpeech())
                                .cefrLevel(s.getCefrLevel())
                                .isAiCefr(s.getIsAiCefr() != null && s.getIsAiCefr())
                                .definitionEn(s.getDefinitionEn())
                                .definitionVn(s.getDefinitionVn())
                                .exampleEn(s.getExampleEn())
                                .exampleVn(s.getExampleVn())
                                .topic(s.getTopic() != null ? s.getTopic().getName() : null)
                                .topicId(s.getTopic() != null ? s.getTopic().getId() : null)
                                .synonyms(s.getSynonyms().stream()
                                        .map(syn -> VocabularyDetailDTO.SynonymDTO.builder()
                                                .synonymWord(syn.getSynonymWord())
                                                .synonymVn(syn.getSynonymVn())
                                                .build())
                                        .collect(Collectors.toList()))
                                .antonyms(s.getAntonyms().stream()
                                        .map(ant -> VocabularyDetailDTO.AntonymDTO.builder()
                                                .antonymWord(ant.getAntonymWord())
                                                .antonymVn(ant.getAntonymVn())
                                                .build())
                                        .collect(Collectors.toList()))
                                .collocations(s.getCollocations().stream()
                                        .map(col -> VocabularyDetailDTO.CollocationDTO.builder()
                                                .collocationWord(col.getCollocationWord())
                                                .collocationVn(col.getCollocationVn())
                                                .build())
                                        .collect(Collectors.toList()))
                                .build())
                        .collect(Collectors.toList()))
                .idioms(entity.getIdioms().stream()
                        .map(i -> VocabularyDetailDTO.IdiomDTO.builder()
                                .phrase(i.getPhrase())
                                .definition(i.getDefinition())
                                .definitionVn(i.getDefinitionVn())
                                .example(i.getExample())
                                .exampleVn(i.getExampleVn())
                                .build())
                        .collect(Collectors.toList()))
                .phrasalVerbs(entity.getPhrasalVerbs().stream()
                        .map(pv -> VocabularyDetailDTO.PhrasalVerbDTO.builder()
                                .phrase(pv.getPhrase())
                                .definition(pv.getDefinition())
                                .definitionVn(pv.getDefinitionVn())
                                .example(pv.getExample())
                                .exampleVn(pv.getExampleVn())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
