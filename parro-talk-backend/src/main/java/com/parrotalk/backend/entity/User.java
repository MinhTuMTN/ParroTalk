package com.parrotalk.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.parrotalk.backend.constant.Role;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * User entity
 * 
 * @author MinhTuMTN
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE users SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class User extends BaseEntity implements UserDetails {

    /**
     * User ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User email
     */
    @Column(unique = true, nullable = false)
    @Email
    @NotBlank
    private String email;

    /**
     * User password
     */
    @Column(nullable = false)
    @NotBlank
    private String password;

    /**
     * User role
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * User full name
     */
    @Column(nullable = false)
    @NotBlank
    private String fullName;

    /**
     * User enabled
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = false;

    /**
     * Get user authorities
     */
    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Get username
     */
    @JsonIgnore
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Check if account is non expired
     */
    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Check if account is non locked
     */
    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Check if credentials are non expired
     */
    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Check if user is enabled
     */
    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return enabled && !isDeleted();
    }
}
