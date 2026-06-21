package com.arenagamer.api.security;

import com.arenagamer.api.entity.enums.AuthUserType;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class UserPrincipal {

    private UserPrincipal() {}

    public static AuthenticatedUser current() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new IllegalStateException("No authenticated user");
    }

    public static Long currentId() {
        return current().getId();
    }

    public static AuthUserType currentType() {
        return current().getType();
    }

    public static Integer currentContactId() {
        AuthenticatedUser user = current();
        if (!user.isContact()) {
            throw new IllegalStateException("Authenticated user is not a contact");
        }
        return user.getId().intValue();
    }

    public static Optional<AuthenticatedUser> tryCurrent() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }
}
