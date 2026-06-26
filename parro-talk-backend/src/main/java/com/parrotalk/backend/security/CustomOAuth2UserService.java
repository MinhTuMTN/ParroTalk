package com.parrotalk.backend.security;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parrotalk.backend.constant.AuthProvider;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserOAuthAccount;
import com.parrotalk.backend.repository.UserOAuthAccountRepository;
import com.parrotalk.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * OAuth2 user service for Google accounts.
 * 
 * @author MinhTuMTN
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    /** User repository */
    private final UserRepository userRepository;

    /** User OAuth account repository */
    private final UserOAuthAccountRepository userOAuthAccountRepository;

    /** OIDC user service delegate */
    private final OidcUserService delegate = new OidcUserService();

    /**
     * Load user from OAuth2 provider.
     *
     * @param userRequest OAuth2 user request
     * @return OAuth2 user
     * @throws OAuth2AuthenticationException If the user cannot be loaded
     */
    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);

        String providerId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        Boolean emailVerified = oidcUser.getEmailVerified();
        String fullName = oidcUser.getFullName();
        String avatarUrl = oidcUser.getPicture();

        if (providerId == null || providerId.isBlank()) {
            throw oauthException("invalid_google_subject", "Google account is missing a stable subject identifier");
        }

        if (email == null || email.isBlank()) {
            throw oauthException("invalid_google_email", "Google account is missing an email address");
        }

        if (!Boolean.TRUE.equals(emailVerified)) {
            throw oauthException("unverified_google_email", "Google email address is not verified");
        }

        User user = userOAuthAccountRepository
                .findByProviderAndProviderId(AuthProvider.GOOGLE, providerId)
                .map(UserOAuthAccount::getUser)
                .orElseGet(() -> linkGoogleAccount(email, providerId, fullName, avatarUrl));

        updateUserProfile(user, fullName, avatarUrl);

        return new ParroTalkOidcUser(
                oidcUser.getAuthorities(),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "sub",
                user);
    }

    private User linkGoogleAccount(String email, String providerId, String fullName, String avatarUrl) {
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .fullName(resolveFullName(fullName, email))
                        .avatarUrl(avatarUrl)
                        .role(Role.USER)
                        .enabled(true)
                        .emailVerified(true)
                        .emailVerifiedAt(LocalDateTime.now())
                        .build()));

        var existingAccount = userOAuthAccountRepository.findByUserAndProvider(user, AuthProvider.GOOGLE);
        existingAccount.ifPresent(account -> {
            if (!account.getProviderId().equals(providerId)) {
                throw oauthException(
                        "google_account_already_linked",
                        "This user already has a different Google account linked");
            }
        });

        if (existingAccount.isEmpty()) {
            userOAuthAccountRepository.save(UserOAuthAccount.builder()
                    .user(user)
                    .provider(AuthProvider.GOOGLE)
                    .providerId(providerId)
                    .build());
        }

        return user;
    }

    private void updateUserProfile(User user, String fullName, String avatarUrl) {
        String resolvedFullName = resolveFullName(fullName, user.getEmail());
        boolean changed = false;

        if (!Objects.equals(user.getFullName(), resolvedFullName)) {
            user.setFullName(resolvedFullName);
            changed = true;
        }

        if (!Objects.equals(user.getAvatarUrl(), avatarUrl)) {
            user.setAvatarUrl(avatarUrl);
            changed = true;
        }

        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(LocalDateTime.now());
            changed = true;
        }

        if (changed) {
            userRepository.save(user);
        }
    }

    private String resolveFullName(String fullName, String email) {
        return fullName == null || fullName.isBlank() ? email : fullName;
    }

    private OAuth2AuthenticationException oauthException(String code, String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(code), description);
    }
}
