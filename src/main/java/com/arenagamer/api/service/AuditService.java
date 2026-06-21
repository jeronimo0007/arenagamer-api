package com.arenagamer.api.service;

import com.arenagamer.api.entity.AuditLog;
import com.arenagamer.api.entity.enums.AuthUserType;
import com.arenagamer.api.repository.AuditLogRepository;
import com.arenagamer.api.security.AuthenticatedUser;
import com.arenagamer.api.security.UserPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public void recordStaffAction(String action, String entityType, Long entityId, Object oldValue, Object newValue) {
        UserPrincipal.tryCurrent()
                .filter(AuthenticatedUser::isStaff)
                .ifPresent(actor -> persist(actor, action, entityType, entityId, oldValue, newValue));
    }

    public void recordStaffAction(AuthenticatedUser actor, String action, String entityType, Long entityId,
                                  Object oldValue, Object newValue) {
        if (actor == null || !actor.isStaff()) {
            return;
        }
        persist(actor, action, entityType, entityId, oldValue, newValue);
    }

    public void recordStaffMessage(String action, String entityType, Long entityId, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", message);
        recordStaffAction(action, entityType, entityId, null, payload);
    }

    public void recordStaffMessage(AuthenticatedUser actor, String action, String entityType, Long entityId,
                                   String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", message);
        recordStaffAction(actor, action, entityType, entityId, null, payload);
    }

    private void persist(AuthenticatedUser actor, String action, String entityType, Long entityId,
                         Object oldValue, Object newValue) {
        HttpServletRequest request = currentRequest().orElse(null);

        AuditLog log = AuditLog.builder()
                .actorType(AuthUserType.STAFF)
                .actorId(actor.getId())
                .action(normalizeAction(action))
                .entityType(normalizeEntityType(entityType))
                .entityId(entityId)
                .oldValue(toJson(oldValue))
                .newValue(toJson(newValue))
                .ipAddress(resolveIp(request))
                .userAgent(resolveUserAgent(request))
                .build();

        auditLogRepository.save(log);
    }

    private Optional<HttpServletRequest> currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Optional.empty();
        }
        return Optional.of(attributes.getRequest());
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUserAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
    }

    private String normalizeAction(String action) {
        return action == null ? "UNKNOWN" : action.trim().toUpperCase();
    }

    private String normalizeEntityType(String entityType) {
        return entityType == null ? "unknown" : entityType.trim().toLowerCase();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String str) {
            return str;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{\"message\":\"" + String.valueOf(value).replace("\"", "'") + "\"}";
        }
    }
}
