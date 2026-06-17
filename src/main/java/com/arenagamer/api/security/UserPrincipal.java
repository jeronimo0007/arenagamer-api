package com.arenagamer.api.security;

import com.arenagamer.api.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;

public final class UserPrincipal {

    private UserPrincipal() {}

    public static User current() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        throw new IllegalStateException("No authenticated user");
    }

    public static Long currentId() {
        return current().getId();
    }
}
