package com.parrotalk.backend.repository;

import com.parrotalk.backend.client.ResendClient;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserSettings;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserSettingsRepositoryTest {

    @MockitoBean
    private ResendClient resendClient;

    @MockitoBean
    private ConnectionFactory connectionFactory;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Saves default UserSettings, loads via findByUserId, and persists updates against Hibernate ORM")
    void saveAndFindByUserId_PersistsDefaultsAndUpdates() {
        User user = userRepository.saveAndFlush(User.builder()
                .email("repo-settings-" + UUID.randomUUID().toString().substring(0, 6) + "@parrotalk.fun")
                .displayUsername("repo_user_" + UUID.randomUUID().toString().substring(0, 6))
                .fullName("Repo Settings User")
                .password("$2a$10$hash")
                .role(Role.USER)
                .enabled(true)
                .emailVerified(true)
                .build());

        UserSettings defaults = UserSettings.createDefaults(user.getId());
        UserSettings saved = userSettingsRepository.saveAndFlush(defaults);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.isDeleted()).isFalse();

        entityManager.clear();

        Optional<UserSettings> loadedOpt = userSettingsRepository.findByUserId(user.getId());
        assertThat(loadedOpt).isPresent();
        UserSettings loaded = loadedOpt.get();
        assertThat(loaded.getUserId()).isEqualTo(user.getId());
        assertThat(loaded.getUiLanguage()).isEqualTo("vi");
        assertThat(loaded.getTranslationLanguage()).isEqualTo("vi");
        assertThat(loaded.getTheme()).isEqualTo("light");
        assertThat(loaded.getPreferredAccent()).isEqualTo("US");
        assertThat(loaded.getPreferredUsVoice()).isEqualTo("en-US-JennyNeural");
        assertThat(loaded.getPreferredUkVoice()).isEqualTo("en-GB-SoniaNeural");
        assertThat(loaded.getPlaybackSpeed()).isEqualTo(1.0);
        assertThat(loaded.getAutoPlayVocabAudio()).isFalse();
        assertThat(loaded.getDictationAutoAdvance()).isFalse();
        assertThat(loaded.getDictationShowTranslation()).isTrue();
        assertThat(loaded.getDictationAutoReplay()).isFalse();
        assertThat(loaded.getDictationReplayKey()).isEqualTo("Escape");
        assertThat(loaded.getDefaultWritingMode()).isEqualTo("SUGGESTION");
        assertThat(loaded.getDefaultDifficultyScale()).isEqualTo("CEFR");
        assertThat(loaded.getDefaultTargetLevel()).isEqualTo("B2");
        assertThat(loaded.getVocabSessionSize()).isEqualTo(10);
        assertThat(loaded.getDefaultFlashcardDirection()).isEqualTo("EN_VI");
        assertThat(loaded.getDailyGoal()).isEqualTo(20);

        loaded.setTheme("dark");
        loaded.setPreferredAccent("UK");
        loaded.setPreferredUkVoice("en-GB-RyanNeural");
        loaded.setPlaybackSpeed(1.25);
        loaded.setDailyGoal(35);
        userSettingsRepository.saveAndFlush(loaded);
        entityManager.clear();

        UserSettings updated = userSettingsRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(updated.getTheme()).isEqualTo("dark");
        assertThat(updated.getPreferredAccent()).isEqualTo("UK");
        assertThat(updated.getPreferredUkVoice()).isEqualTo("en-GB-RyanNeural");
        assertThat(updated.getPlaybackSpeed()).isEqualTo(1.25);
        assertThat(updated.getDailyGoal()).isEqualTo(35);
    }

    @Test
    @DisplayName("Enforces unique user_id constraint on user_settings table")
    void saveDuplicateUserId_ThrowsDataIntegrityViolationException() {
        UUID userId = UUID.randomUUID();
        userSettingsRepository.saveAndFlush(UserSettings.createDefaults(userId));

        UserSettings duplicate = UserSettings.createDefaults(userId);
        assertThatThrownBy(() -> {
            userSettingsRepository.saveAndFlush(duplicate);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
