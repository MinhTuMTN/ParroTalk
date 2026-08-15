package com.parrotalk.backend.service;

import com.parrotalk.backend.constant.CacheNames;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }


    public Optional<User> findByEmailIncludeDeleted(String email) {
        return userRepository.findByEmailIncludeDeleted(email);
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @CacheEvict(value = CacheNames.USER_CACHE, key = "#user.email")
    public User save(User user) {
        return userRepository.save(user);
    }

    @CacheEvict(value = CacheNames.USER_CACHE, key = "#user.email")
    public User saveAndFlush(User user) {
        return userRepository.saveAndFlush(user);
    }

    /**
     * Updates user's last active timestamp and saves to database while evicting relevant caches.
     *
     * @param user User entity to update
     * @return Updated User entity
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheNames.USER_CACHE, key = "#user.email"),
        @CacheEvict(value = CacheNames.ADMIN_USER_DETAIL_CACHE, key = "#user.id")
    })
    public User updateLastActiveAt(User user) {
        user.setLastActiveAt(LocalDateTime.now());
        return userRepository.save(user);
    }
}
