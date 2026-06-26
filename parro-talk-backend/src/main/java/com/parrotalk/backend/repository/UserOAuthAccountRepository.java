package com.parrotalk.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parrotalk.backend.constant.AuthProvider;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserOAuthAccount;

@Repository
public interface UserOAuthAccountRepository extends JpaRepository<UserOAuthAccount, UUID> {
    Optional<UserOAuthAccount> findByProviderAndProviderId(AuthProvider provider, String providerId);

    Optional<UserOAuthAccount> findByUserAndProvider(User user, AuthProvider provider);
}
