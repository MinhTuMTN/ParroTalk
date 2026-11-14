package com.parrotalk.backend.controller;

import com.parrotalk.backend.ParroTalkBackendApplication;
import com.parrotalk.backend.client.ResendClient;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.repository.UserRepository;
import com.parrotalk.backend.repository.UserSettingsRepository;
import com.parrotalk.backend.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ParroTalkBackendApplication.class)
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    @MockitoBean
    private ResendClient resendClient;

    @MockitoBean
    private ConnectionFactory connectionFactory;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;
    private User passwordUser;
    private User oauthUser;
    private String passwordUserAuthHeader;
    private String oauthUserAuthHeader;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String suffix = UUID.randomUUID().toString().substring(0, 6);
        passwordUser = userRepository.saveAndFlush(User.builder()
                .email("settings-user-" + suffix + "@parrotalk.fun")
                .displayUsername("learner_" + suffix)
                .fullName("Local Learner")
                .password(passwordEncoder.encode("Secret123!"))
                .avatarUrl("https://cdn.parrotalk.fun/avatar.png")
                .role(Role.USER)
                .enabled(true)
                .emailVerified(true)
                .build());

        oauthUser = userRepository.saveAndFlush(User.builder()
                .email("oauth-user-" + suffix + "@parrotalk.fun")
                .displayUsername("oauth_" + suffix)
                .fullName("Google Learner")
                .password(null)
                .role(Role.USER)
                .enabled(true)
                .emailVerified(true)
                .build());

        passwordUserAuthHeader = "Bearer " + jwtUtils.generateToken(passwordUser);
        oauthUserAuthHeader = "Bearer " + jwtUtils.generateToken(oauthUser);
    }

    @Test
    @DisplayName("User settings/profile/password endpoints require authentication")
    void endpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me/settings")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/users/me/settings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/users/me/profile").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/users/me/password").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/users/profile and GET /api/auth/me expose username, avatarUrl, and hasPassword")
    void profileAndAuthMeExposeHasPasswordAndEnrichedFields() throws Exception {
        mockMvc.perform(get("/api/users/profile").header("Authorization", passwordUserAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(passwordUser.getEmail()))
                .andExpect(jsonPath("$.username").value(passwordUser.getDisplayUsername()))
                .andExpect(jsonPath("$.avatarUrl").value("https://cdn.parrotalk.fun/avatar.png"))
                .andExpect(jsonPath("$.hasPassword").value(true));

        mockMvc.perform(get("/api/users/profile").header("Authorization", oauthUserAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(oauthUser.getDisplayUsername()))
                .andExpect(jsonPath("$.hasPassword").value(false));

        mockMvc.perform(get("/api/auth/me").header("Authorization", oauthUserAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.hasPassword").value(false));
    }

    @Test
    @DisplayName("GET and PUT /api/users/me/settings return defaults, upsert partial updates, clamp numbers, and validate allowlists")
    void getAndUpdateSettings_SuccessAndValidation() throws Exception {
        // Initial read returns defaults without persisting a row
        mockMvc.perform(get("/api/users/me/settings").header("Authorization", passwordUserAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uiLanguage").value("vi"))
                .andExpect(jsonPath("$.preferredAccent").value("US"))
                .andExpect(jsonPath("$.preferredUsVoice").value("en-US-JennyNeural"))
                .andExpect(jsonPath("$.preferredUkVoice").value("en-GB-SoniaNeural"))
                .andExpect(jsonPath("$.playbackSpeed").value(1.0))
                .andExpect(jsonPath("$.dictationShowTranslation").value(true))
                .andExpect(jsonPath("$.defaultWritingMode").value("SUGGESTION"))
                .andExpect(jsonPath("$.defaultDifficultyScale").value("CEFR"))
                .andExpect(jsonPath("$.defaultTargetLevel").value("B2"))
                .andExpect(jsonPath("$.vocabSessionSize").value(10))
                .andExpect(jsonPath("$.defaultFlashcardDirection").value("EN_VI"))
                .andExpect(jsonPath("$.dailyGoal").value(20));

        assertTrue(userSettingsRepository.findByUserId(passwordUser.getId()).isEmpty());

        String partialPayload = """
                {
                  "theme": "dark",
                  "preferredAccent": "UK",
                  "preferredUkVoice": "en-GB-RyanNeural",
                  "playbackSpeed": 2.0,
                  "dictationAutoAdvance": true,
                  "defaultDifficultyScale": "IELTS",
                  "defaultTargetLevel": "7.5",
                  "vocabSessionSize": 999,
                  "dailyGoal": 2
                }
                """;

        mockMvc.perform(put("/api/users/me/settings")
                        .header("Authorization", passwordUserAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partialPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("dark"))
                .andExpect(jsonPath("$.preferredAccent").value("UK"))
                .andExpect(jsonPath("$.preferredUkVoice").value("en-GB-RyanNeural"))
                .andExpect(jsonPath("$.playbackSpeed").value(1.5))
                .andExpect(jsonPath("$.dictationAutoAdvance").value(true))
                .andExpect(jsonPath("$.defaultDifficultyScale").value("IELTS"))
                .andExpect(jsonPath("$.defaultTargetLevel").value("7.5"))
                .andExpect(jsonPath("$.vocabSessionSize").value(50))
                .andExpect(jsonPath("$.dailyGoal").value(5))
                .andExpect(jsonPath("$.uiLanguage").value("vi"));

        // Verify persisted on subsequent GET
        mockMvc.perform(get("/api/users/me/settings").header("Authorization", passwordUserAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("dark"))
                .andExpect(jsonPath("$.preferredUkVoice").value("en-GB-RyanNeural"));

        // Invalid voice rejected with 400 VALIDATION_ERROR
        mockMvc.perform(put("/api/users/me/settings")
                        .header("Authorization", passwordUserAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredUsVoice\": \"en-US-UnknownNeural\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PUT /api/users/me/profile updates profile and rejects duplicate username")
    void updateProfile_SuccessAndDuplicateUsernameRejected() throws Exception {
        String payload = """
                {
                  "fullName": "Updated Learner",
                  "username": "new_unique.name",
                  "avatarUrl": "/uploads/new-avatar.jpg"
                }
                """;

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", passwordUserAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(passwordUser.getEmail()))
                .andExpect(jsonPath("$.fullName").value("Updated Learner"))
                .andExpect(jsonPath("$.username").value("new_unique.name"))
                .andExpect(jsonPath("$.avatarUrl").value("/uploads/new-avatar.jpg"))
                .andExpect(jsonPath("$.hasPassword").value(true));

        // Duplicate username belonging to oauthUser is rejected
        String dupPayload = """
                {
                  "fullName": "Updated Learner",
                  "username": "%s"
                }
                """.formatted(oauthUser.getDisplayUsername());

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", passwordUserAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dupPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_USERNAME"));
    }

    @Test
    @DisplayName("PUT /api/users/me/password verifies old password for password users and allows first password for OAuth users")
    void changePassword_PasswordAndOAuthUsers() throws Exception {
        // Wrong current password rejected
        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", passwordUserAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "WrongPassword!",
                                  "newPassword": "NewPassword123!",
                                  "confirmPassword": "NewPassword123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));

        // Correct current password succeeds
        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", passwordUserAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "Secret123!",
                                  "newPassword": "NewPassword123!",
                                  "confirmPassword": "NewPassword123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));

        User updatedPasswordUser = userRepository.findById(passwordUser.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("NewPassword123!", updatedPasswordUser.getPassword()));

        // OAuth passwordless user can set first local password without currentPassword
        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", oauthUserAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newPassword": "FirstLocalPassword1!",
                                  "confirmPassword": "FirstLocalPassword1!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));

        mockMvc.perform(get("/api/users/profile").header("Authorization", oauthUserAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPassword").value(true));
    }

    @Test
    @DisplayName("CORS preflight and authenticated PUT succeed for /api/users/me/* from allowed origin while unauthenticated PUT returns 401")
    void corsPreflightAllowsPutForUserEndpoints() throws Exception {
        mockMvc.perform(options("/api/users/me/settings")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PUT")));

        mockMvc.perform(options("/api/users/me/profile")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PUT")));

        mockMvc.perform(options("/api/users/me/password")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PUT")));

        mockMvc.perform(put("/api/users/me/settings")
                        .header("Origin", "http://localhost:3000")
                        .header("Authorization", passwordUserAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"theme\": \"dark\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(jsonPath("$.theme").value("dark"));

        mockMvc.perform(put("/api/users/me/settings")
                        .header("Origin", "http://localhost:3000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"theme\": \"dark\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/users/me/profile preserves omitted username and avatarUrl, clears explicit empty avatarUrl, and rejects blank username")
    void updateProfile_NullUsernamePreservedAndEmptyOrBlankRejected() throws Exception {
        String existingUsername = passwordUser.getDisplayUsername();
        String existingAvatarUrl = passwordUser.getAvatarUrl();

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", passwordUserAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\": \"Name Without Username\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Name Without Username"))
                .andExpect(jsonPath("$.username").value(existingUsername))
                .andExpect(jsonPath("$.avatarUrl").value(existingAvatarUrl));

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", passwordUserAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\": \"Cleared Avatar\", \"avatarUrl\": \"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Cleared Avatar"))
                .andExpect(jsonPath("$.username").value(existingUsername))
                .andExpect(jsonPath("$.avatarUrl").value(nullValue()));

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", passwordUserAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\": \"Blank Username\", \"username\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", passwordUserAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\": \"Empty Username\", \"username\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/auth/login and POST /api/auth/refresh include user.hasPassword accurately")
    void authLoginAndRefreshIncludeHasPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Secret123!"
                                }
                                """.formatted(passwordUser.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.user.hasPassword").value(true));

        String oauthRefreshToken = jwtUtils.generateRefreshToken(oauthUser);
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(oauthRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.user.hasPassword").value(false));
    }
}
