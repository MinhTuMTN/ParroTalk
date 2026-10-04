package com.parrotalk.backend.service.admin;

import com.parrotalk.backend.dto.admin.vocab.AdminVocabularyDetailDto;
import com.parrotalk.backend.dto.admin.vocab.AdminVocabularyListDto;
import com.parrotalk.backend.dto.admin.vocab.AdminVocabularyRequestDto;
import com.parrotalk.backend.entity.*;
import com.parrotalk.backend.repository.TopicRepository;
import com.parrotalk.backend.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCoreVocabularyService {

    private final VocabularyRepository vocabularyRepository;
    private final TopicRepository topicRepository;

    @Transactional(readOnly = true)
    public Page<AdminVocabularyListDto> getVocabularies(String search, String level, String partOfSpeech, UUID topicId, Pageable pageable) {
        Specification<Vocabulary> spec = Specification.where((root, query, cb) -> cb.conjunction());
        if (search != null && !search.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("word")), "%" + search.toLowerCase() + "%"));
        }
        if (topicId != null) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                var sensesJoin = root.join("senses", jakarta.persistence.criteria.JoinType.INNER);
                return cb.equal(sensesJoin.get("topic").get("id"), topicId);
            });
        }

        return vocabularyRepository.findAll(spec, pageable).map(this::toListDto);
    }

    @Transactional(readOnly = true)
    public AdminVocabularyDetailDto getVocabulary(UUID id) {
        Vocabulary vocab = vocabularyRepository.findByIdWithGraph(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vocabulary not found"));
        return toDetailDto(vocab);
    }

    @Transactional
    public AdminVocabularyDetailDto createVocabulary(AdminVocabularyRequestDto request) {
        Vocabulary vocab = new Vocabulary();
        vocab.setWord(request.getWord());
        vocab.setNormalizedWord(request.getWord().toLowerCase().trim());
        vocab.setCommonMeaningVn(request.getCommonMeaningVn());

        updateNestedEntities(vocab, request);
        return toDetailDto(vocabularyRepository.save(vocab));
    }

    @Transactional
    public AdminVocabularyDetailDto updateVocabulary(UUID id, AdminVocabularyRequestDto request) {
        Vocabulary vocab = vocabularyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vocabulary not found"));
        
        vocab.setWord(request.getWord());
        vocab.setNormalizedWord(request.getWord().toLowerCase().trim());
        vocab.setCommonMeaningVn(request.getCommonMeaningVn());

        updateNestedEntities(vocab, request);
        return toDetailDto(vocabularyRepository.save(vocab));
    }

    @Transactional
    public void deleteVocabulary(UUID id) {
        if (!vocabularyRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vocabulary not found");
        }
        vocabularyRepository.deleteById(id);
    }

    private void updateNestedEntities(Vocabulary vocab, AdminVocabularyRequestDto request) {
        // Senses
        vocab.getSenses().clear();
        if (request.getSenses() != null) {
            for (var senseReq : request.getSenses()) {
                VocabularySense sense = new VocabularySense();
                sense.setVocabulary(vocab);
                sense.setPartOfSpeech(senseReq.getPartOfSpeech());
                sense.setCefrLevel(senseReq.getCefrLevel());
                sense.setIsAiCefr(senseReq.getIsAiCefr() != null ? senseReq.getIsAiCefr() : false);
                sense.setDefinitionEn(senseReq.getDefinitionEn());
                sense.setDefinitionVn(senseReq.getDefinitionVn());
                sense.setExampleEn(senseReq.getExampleEn());
                sense.setExampleVn(senseReq.getExampleVn());
                
                // Resolve topic: prefer topicId, then topic name, then topics[0]
                UUID reqTopicId = senseReq.getTopicId();
                String reqTopicName = senseReq.getTopic();
                if (reqTopicName == null && senseReq.getTopics() != null && !senseReq.getTopics().isEmpty()) {
                    reqTopicName = senseReq.getTopics().get(0);
                }

                if (reqTopicId != null) {
                    topicRepository.findById(reqTopicId).ifPresent(sense::setTopic);
                } else if (reqTopicName != null && !reqTopicName.trim().isEmpty()) {
                    final String trimmedName = reqTopicName.trim();
                    Topic topic = topicRepository.findByName(trimmedName).orElseGet(() -> {
                        Topic newTopic = new Topic();
                        newTopic.setName(trimmedName);
                        return topicRepository.save(newTopic);
                    });
                    sense.setTopic(topic);
                }

                if (senseReq.getSynonyms() != null) {
                    sense.setSynonyms(senseReq.getSynonyms().stream().map(s -> {
                        SenseSynonym syn = new SenseSynonym();
                        syn.setSense(sense);
                        syn.setSynonymWord(s);
                        return syn;
                    }).collect(Collectors.toList()));
                }
                
                if (senseReq.getAntonyms() != null) {
                    sense.setAntonyms(senseReq.getAntonyms().stream().map(s -> {
                        SenseAntonym ant = new SenseAntonym();
                        ant.setSense(sense);
                        ant.setAntonymWord(s);
                        return ant;
                    }).collect(Collectors.toList()));
                }

                if (senseReq.getCollocations() != null) {
                    sense.setCollocations(senseReq.getCollocations().stream().map(s -> {
                        SenseCollocation col = new SenseCollocation();
                        col.setSense(sense);
                        col.setCollocationWord(s);
                        return col;
                    }).collect(Collectors.toList()));
                }

                vocab.getSenses().add(sense);
            }
        }

        // Pronunciations
        vocab.getPronunciations().clear();
        if (request.getPronunciations() != null) {
            for (var pReq : request.getPronunciations()) {
                VocabularyPronunciation pron = new VocabularyPronunciation();
                pron.setVocabulary(vocab);
                pron.setAccent(pReq.getRegion());
                pron.setIpa(pReq.getIpa());
                pron.setAudioUrl(pReq.getAudioUrl());
                vocab.getPronunciations().add(pron);
            }
        }

        // Idioms
        vocab.getIdioms().clear();
        if (request.getIdioms() != null) {
            for (var iReq : request.getIdioms()) {
                Idiom idiom = new Idiom();
                idiom.setVocabulary(vocab);
                idiom.setPhrase(iReq.getText());
                idiom.setDefinition(iReq.getMeaningEn());
                idiom.setDefinitionVn(iReq.getMeaningVn());
                vocab.getIdioms().add(idiom);
            }
        }

        // Phrasal Verbs
        vocab.getPhrasalVerbs().clear();
        if (request.getPhrasalVerbs() != null) {
            for (var pvReq : request.getPhrasalVerbs()) {
                PhrasalVerb pv = new PhrasalVerb();
                pv.setVocabulary(vocab);
                pv.setPhrase(pvReq.getText());
                pv.setDefinition(pvReq.getMeaningEn());
                pv.setDefinitionVn(pvReq.getMeaningVn());
                vocab.getPhrasalVerbs().add(pv);
            }
        }
    }

    private AdminVocabularyListDto toListDto(Vocabulary vocab) {
        return AdminVocabularyListDto.builder()
                .id(vocab.getId())
                .word(vocab.getWord())
                .commonMeaningVn(vocab.getCommonMeaningVn())
                .senseCount(vocab.getSenses().size())
                .topics(vocab.getSenses().stream()
                        .map(VocabularySense::getTopic)
                        .filter(java.util.Objects::nonNull)
                        .map(Topic::getName)
                        .distinct()
                        .collect(Collectors.toList()))
                .cefrLevels(vocab.getSenses().stream().map(VocabularySense::getCefrLevel).distinct().collect(Collectors.toList()))
                .partsOfSpeech(vocab.getSenses().stream().map(VocabularySense::getPartOfSpeech).distinct().collect(Collectors.toList()))
                .build();
    }

    private AdminVocabularyDetailDto toDetailDto(Vocabulary vocab) {
        return AdminVocabularyDetailDto.builder()
                .id(vocab.getId())
                .word(vocab.getWord())
                .commonMeaningVn(vocab.getCommonMeaningVn())
                .pronunciations(vocab.getPronunciations().stream().map(p -> 
                    AdminVocabularyDetailDto.AdminVocabularyPronunciationDto.builder()
                        .id(p.getId())
                        .region(p.getAccent())
                        .ipa(p.getIpa())
                        .audioUrl(p.getAudioUrl())
                        .build()
                ).collect(Collectors.toList()))
                .senses(vocab.getSenses().stream().map(s -> 
                    AdminVocabularyDetailDto.AdminVocabularySenseDto.builder()
                        .id(s.getId())
                        .partOfSpeech(s.getPartOfSpeech())
                        .cefrLevel(s.getCefrLevel())
                        .isAiCefr(s.getIsAiCefr())
                        .definitionEn(s.getDefinitionEn())
                        .definitionVn(s.getDefinitionVn())
                        .exampleEn(s.getExampleEn())
                        .exampleVn(s.getExampleVn())
                        .topics(s.getTopic() != null ? List.of(s.getTopic().getName()) : new ArrayList<>())
                        .topic(s.getTopic() != null ? s.getTopic().getName() : null)
                        .topicId(s.getTopic() != null ? s.getTopic().getId() : null)
                        .synonyms(s.getSynonyms().stream().map(SenseSynonym::getSynonymWord).collect(Collectors.toList()))
                        .antonyms(s.getAntonyms().stream().map(SenseAntonym::getAntonymWord).collect(Collectors.toList()))
                        .collocations(s.getCollocations().stream().map(SenseCollocation::getCollocationWord).collect(Collectors.toList()))
                        .build()
                ).collect(Collectors.toList()))
                .idioms(vocab.getIdioms().stream().map(i ->
                    AdminVocabularyDetailDto.AdminExpressionDto.builder()
                        .id(i.getId())
                        .text(i.getPhrase())
                        .meaningEn(i.getDefinition())
                        .meaningVn(i.getDefinitionVn())
                        .build()
                ).collect(Collectors.toList()))
                .phrasalVerbs(vocab.getPhrasalVerbs().stream().map(p ->
                    AdminVocabularyDetailDto.AdminExpressionDto.builder()
                        .id(p.getId())
                        .text(p.getPhrase())
                        .meaningEn(p.getDefinition())
                        .meaningVn(p.getDefinitionVn())
                        .build()
                ).collect(Collectors.toList()))
                .build();
    }
}
