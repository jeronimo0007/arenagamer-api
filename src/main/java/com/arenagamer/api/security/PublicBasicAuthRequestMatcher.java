package com.arenagamer.api.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Rotas públicas de catálogo com HTTP Basic (email/senha de staff ou contact):
 * GET /api/v1/public/plans
 * GET /api/v1/public/tournaments
 * GET /api/v1/public/tournaments/**
 * GET /api/v1/public/presets
 * GET /api/v1/public/tournament-pricing
 * GET /api/v1/public/team-settings
 * GET /api/v1/public/teams/**
 * GET /api/v1/public/players/**
 */
@Component
public class PublicBasicAuthRequestMatcher implements RequestMatcher {

    private static final Set<String> CATALOG_PATHS = Set.of(
            "/api/v1/public/plans",
            "/api/v1/public/tournaments",
            "/api/v1/public/presets",
            "/api/v1/public/tournament-pricing",
            "/api/v1/public/team-settings"
    );

    private static final String TEAMS_PREFIX = "/api/v1/public/teams";
    private static final String PLAYERS_PREFIX = "/api/v1/public/players";
    private static final String TOURNAMENTS_PREFIX = "/api/v1/public/tournaments";

    @Override
    public boolean matches(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = requestPath(request);
        return CATALOG_PATHS.contains(path)
                || path.startsWith(TEAMS_PREFIX)
                || path.startsWith(PLAYERS_PREFIX)
                || path.startsWith(TOURNAMENTS_PREFIX);
    }

    private String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        int query = uri.indexOf('?');
        return query >= 0 ? uri.substring(0, query) : uri;
    }
}
