package com.arenagamer.api.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Rotas públicas de catálogo com HTTP Basic (email/senha de staff ou contact):
 * GET /api/v1/public/plans
 * GET /api/v1/public/tournaments
 * GET /api/v1/public/presets
 */
@Component
public class PublicBasicAuthRequestMatcher implements RequestMatcher {

    private static final Set<String> CATALOG_PATHS = Set.of(
            "/api/v1/public/plans",
            "/api/v1/public/tournaments",
            "/api/v1/public/presets"
    );

    @Override
    public boolean matches(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        return CATALOG_PATHS.contains(requestPath(request));
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
