package com.parrotalk.backend.service;

import com.parrotalk.backend.constant.PracticeQuestionType;
import com.parrotalk.backend.constant.PracticeSessionStatus;
import com.parrotalk.backend.constant.Sm2Rating;
import com.parrotalk.backend.constant.VocabularyStatus;
import com.parrotalk.backend.dto.practice.AnswerResultDto;
import com.parrotalk.backend.dto.practice.AnswerSubmissionDto;
import com.parrotalk.backend.dto.practice.PracticeQuestionDto;
import com.parrotalk.backend.dto.practice.PracticeResultDto;
import com.parrotalk.backend.dto.practice.PracticeSessionDto;
import com.parrotalk.backend.dto.practice.PracticeStatisticsDto;
import com.parrotalk.backend.entity.DictionaryEntry;
import com.parrotalk.backend.entity.PracticeAnswer;
import com.parrotalk.backend.entity.PracticeQuestion;
import com.parrotalk.backend.entity.PracticeSession;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserVocabulary;
import com.parrotalk.backend.repository.PracticeAnswerRepository;
import com.parrotalk.backend.repository.PracticeQuestionRepository;
import com.parrotalk.backend.repository.PracticeSessionRepository;
import com.parrotalk.backend.repository.UserRepository;
import com.parrotalk.backend.repository.UserVocabularyRepository;
import com.parrotalk.backend.repository.cms.AdminVocabularyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeService {

    private final PracticeSessionRepository sessionRepository;
    private final PracticeAnswerRepository answerRepository;
    private final PracticeQuestionRepository questionRepository;
    private final UserVocabularyRepository vocabularyRepository;
    private final UserRepository userRepository;
    private final AdminVocabularyRepository adminVocabularyRepository;
    private final Sm2Algorithm sm2Algorithm;
    private final ObjectMapper objectMapper;
    
    private final Random random = new Random();
    private static final int SESSION_SIZE = 20;

    /**
     * Generates a new practice session for a user.
     */
    @Transactional
    public PracticeSessionDto generateSession(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 1. Fetch due items
        PageRequest pageRequest = PageRequest.of(0, SESSION_SIZE, Sort.by("nextReviewAt").ascending());
        Page<UserVocabulary> dueItemsPage = vocabularyRepository.findByUserIdAndNextReviewAtLessThanEqual(userId, LocalDateTime.now(), pageRequest);
        List<UserVocabulary> selectedItems = new ArrayList<>(dueItemsPage.getContent());

        // 2. If less than SESSION_SIZE, fetch NEW items
        if (selectedItems.size() < SESSION_SIZE) {
            int remaining = SESSION_SIZE - selectedItems.size();
            PageRequest newItemsRequest = PageRequest.of(0, remaining, Sort.by("createdAt").descending());
            Page<UserVocabulary> newItemsPage = vocabularyRepository.findByUserIdAndStatus(userId, VocabularyStatus.NEW, newItemsRequest);
            selectedItems.addAll(newItemsPage.getContent());
        }

        if (selectedItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No vocabulary available for practice");
        }

        // Create Session
        PracticeSession session = PracticeSession.builder()
                .user(user)
                .startedAt(LocalDateTime.now())
                .totalQuestions(selectedItems.size())
                .status(PracticeSessionStatus.IN_PROGRESS)
                .build();
        session = sessionRepository.save(session);

        // Generate and Save Questions
        List<String> randomWordsPool = adminVocabularyRepository.findRandomWords(50);
        if (randomWordsPool == null || randomWordsPool.isEmpty()) {
            randomWordsPool = List.of("apple", "banana", "cat", "dog", "elephant", "fish", "grape", "hat"); // Fallback
        }

        List<PracticeQuestion> practiceQuestions = new ArrayList<>();
        for (UserVocabulary vocab : selectedItems) {
            practiceQuestions.add(createQuestion(session, vocab, randomWordsPool));
        }
        practiceQuestions = questionRepository.saveAll(practiceQuestions);

        // Map to DTO
        List<PracticeQuestionDto> questionDtos = practiceQuestions.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PracticeSessionDto.builder()
                .sessionId(session.getId())
                .status(session.getStatus())
                .startedAt(session.getStartedAt())
                .questions(questionDtos)
                .build();
    }

    private PracticeQuestion createQuestion(PracticeSession session, UserVocabulary vocab, List<String> randomWordsPool) {
        DictionaryEntry entry = vocab.getDictionaryEntry();
        PracticeQuestionType[] types = PracticeQuestionType.values();
        PracticeQuestionType randomType = types[random.nextInt(types.length)];
        
        if (randomType == PracticeQuestionType.LISTENING && (entry == null || (entry.getAudioUkUrl() == null && entry.getAudioUsUrl() == null))) {
            randomType = PracticeQuestionType.FLASHCARD;
        }

        PracticeQuestion question = PracticeQuestion.builder()
                .session(session)
                .userVocabulary(vocab)
                .questionType(randomType)
                .build();

        if (randomType == PracticeQuestionType.MULTIPLE_CHOICE) {
            List<String> options = new ArrayList<>();
            options.add(vocab.getDisplayWord());
            
            // Pick 3 random distinct distractors from the pool
            Collections.shuffle(randomWordsPool);
            for (String word : randomWordsPool) {
                if (!word.equalsIgnoreCase(vocab.getDisplayWord()) && options.size() < 4) {
                    options.add(word);
                }
            }
            // If pool didn't have enough distinct words, add fallbacks
            while (options.size() < 4) {
                options.add(generateFallbackDummyWord());
            }

            Collections.shuffle(options);
            try {
                question.setOptionsJson(objectMapper.writeValueAsString(options));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize options for practice question", e);
            }
        }
        return question;
    }

    private PracticeQuestionDto mapToDto(PracticeQuestion question) {
        UserVocabulary vocab = question.getUserVocabulary();
        DictionaryEntry entry = vocab.getDictionaryEntry();

        PracticeQuestionDto.PracticeQuestionDtoBuilder builder = PracticeQuestionDto.builder()
                .userVocabularyId(vocab.getId())
                .word(vocab.getNormalizedWord())
                .displayWord(vocab.getDisplayWord())
                .questionType(question.getQuestionType());

        if (entry != null) {
            builder.phonetic(entry.getPhonetic())
                   .audioUrl(entry.getAudioUsUrl() != null ? entry.getAudioUsUrl() : entry.getAudioUkUrl())
                   .partOfSpeech(entry.getPartOfSpeech())
                   .definition(entry.getCommonMeaningVi());
        } else {
            builder.definition(vocab.getNote());
        }

        if (question.getQuestionType() == PracticeQuestionType.MULTIPLE_CHOICE && question.getOptionsJson() != null) {
            try {
                List<String> options = objectMapper.readValue(question.getOptionsJson(), new TypeReference<>() {});
                builder.options(options);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize options for practice question", e);
            }
        } else if (question.getQuestionType() == PracticeQuestionType.SENTENCE_FILL) {
            builder.sentenceTemplate("This is an example sentence for _____.");
        }

        return builder.build();
    }

    private String generateFallbackDummyWord() {
        String[] dummies = {"apple", "banana", "cat", "dog", "elephant", "fish", "grape", "hat"};
        return dummies[random.nextInt(dummies.length)];
    }

    @Transactional(readOnly = true)
    public PracticeSessionDto getSession(UUID userId, UUID sessionId) {
        PracticeSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (!session.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        List<PracticeQuestion> questions = questionRepository.findBySessionId(sessionId);
        List<PracticeQuestionDto> questionDtos = questions.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PracticeSessionDto.builder()
                .sessionId(session.getId())
                .status(session.getStatus())
                .startedAt(session.getStartedAt())
                .questions(questionDtos)
                .build();
    }

    /**
     * Submits an answer to a question.
     */
    @Transactional
    public AnswerResultDto submitAnswer(UUID userId, AnswerSubmissionDto dto) {
        PracticeSession session = sessionRepository.findById(dto.getSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (!session.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        
        if (session.getStatus() != PracticeSessionStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session is not in progress");
        }

        UserVocabulary vocab = vocabularyRepository.findById(dto.getUserVocabularyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vocabulary not found"));

        boolean isCorrect = false;
        int xpEarned = 0;
        
        // Evaluate answer
        if (dto.getRating() != null) { // Flashcard or manual rating
            isCorrect = dto.getRating() == Sm2Rating.GOOD || dto.getRating() == Sm2Rating.EASY;
        } else if (dto.getAnswer() != null) { // Typing, multiple choice, etc
            isCorrect = vocab.getNormalizedWord().equalsIgnoreCase(dto.getAnswer().trim());
            // If not rating provided but correct, assume GOOD, else AGAIN
            dto.setRating(isCorrect ? Sm2Rating.GOOD : Sm2Rating.AGAIN);
        }

        if (isCorrect) {
            session.setCorrectAnswers(session.getCorrectAnswers() + 1);
            xpEarned = 10;
            session.setXpEarned(session.getXpEarned() + xpEarned);
        }

        // Apply SM-2
        sm2Algorithm.calculateNextReview(vocab, dto.getRating() != null ? dto.getRating() : (isCorrect ? Sm2Rating.GOOD : Sm2Rating.AGAIN));
        
        // Update status if needed
        if (vocab.getStatus() == VocabularyStatus.NEW) {
            vocab.setStatus(VocabularyStatus.LEARNING);
        } else if (vocab.getRepetitions() > 5 && vocab.getStatus() == VocabularyStatus.LEARNING) {
            vocab.setStatus(VocabularyStatus.MASTERED);
        }

        vocabularyRepository.save(vocab);

        // Save Answer
        PracticeAnswer answer = PracticeAnswer.builder()
                .session(session)
                .userVocabulary(vocab)
                .questionType(PracticeQuestionType.FLASHCARD) // simplify for now
                .isCorrect(isCorrect)
                .userAnswer(dto.getAnswer())
                .rating(dto.getRating())
                .answeredAt(LocalDateTime.now())
                .timeSpentMs(dto.getTimeSpentMs())
                .build();
        answerRepository.save(answer);

        // Check if session completed
        long answeredCount = answerRepository.countBySessionId(session.getId());
        if (answeredCount >= session.getTotalQuestions()) {
            session.setStatus(PracticeSessionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
        }
        sessionRepository.save(session);

        return AnswerResultDto.builder()
                .correct(isCorrect)
                .correctAnswer(vocab.getDisplayWord())
                .explanation(vocab.getDictionaryEntry() != null ? vocab.getDictionaryEntry().getCommonMeaningVi() : vocab.getNote())
                .xpEarned(xpEarned)
                .build();
    }

    /**
     * Gets result for a completed session.
     */
    @Transactional(readOnly = true)
    public PracticeResultDto getSessionResult(UUID userId, UUID sessionId) {
        PracticeSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (!session.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        double accuracy = session.getTotalQuestions() > 0 
            ? (double) session.getCorrectAnswers() / session.getTotalQuestions() * 100 
            : 0;

        return PracticeResultDto.builder()
                .sessionId(session.getId())
                .totalQuestions(session.getTotalQuestions())
                .correctAnswers(session.getCorrectAnswers())
                .accuracy(accuracy)
                .xpEarned(session.getXpEarned())
                .newMasteredWords(0) // Need more complex logic to track this accurately during session
                .streak(1) // Dummy for now
                .build();
    }

    /**
     * Gets user statistics.
     */
    @Transactional(readOnly = true)
    public PracticeStatisticsDto getStatistics(UUID userId) {
        return PracticeStatisticsDto.builder()
                .todayLearned(15)
                .totalMastered(50)
                .currentStreak(5)
                .reviewDue(12)
                .retentionRate(85.5)
                .build();
    }
}
