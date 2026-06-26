package com.parrotalk.backend.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import com.parrotalk.backend.entity.User;

/**
 * OIDC principal enriched with the local application user.
 */
public class ParroTalkOidcUser extends DefaultOidcUser {

    private final User user;

    public ParroTalkOidcUser(
            Collection<? extends GrantedAuthority> authorities,
            OidcIdToken idToken,
            OidcUserInfo userInfo,
            String nameAttributeKey,
            User user) {
        super(authorities, idToken, userInfo, nameAttributeKey);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
