package com.arenagamer.api.service;

import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.dto.response.NicknameAvailabilityResponse;
import com.arenagamer.api.repository.ClientRepository;
import com.arenagamer.api.util.NicknameRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientNicknameService {

    private final ClientRepository clientRepository;

    public String normalizeRequired(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw BusinessException.badRequest("Nickname é obrigatório");
        }
        String trimmed = nickname.trim();
        validateFormat(trimmed);
        return trimmed;
    }

    public String normalizeOptional(String nickname) {
        if (nickname == null) {
            return null;
        }
        String trimmed = nickname.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        validateFormat(trimmed);
        return trimmed;
    }

    private void validateFormat(String nickname) {
        if (!NicknameRules.isValid(nickname)) {
            throw BusinessException.badRequest(NicknameRules.VALIDATION_MESSAGE);
        }
    }

    public void ensureAvailable(String nickname, Integer excludeClientUserId) {
        if (!isAvailable(nickname, excludeClientUserId)) {
            throw BusinessException.conflict("Nickname já está em uso");
        }
    }

    public boolean isAvailable(String nickname, Integer excludeClientUserId) {
        String normalized = normalizeRequired(nickname);
        return excludeClientUserId == null
                ? !clientRepository.existsByNicknameIgnoreCase(normalized)
                : !clientRepository.existsByNicknameIgnoreCaseAndUserIdNot(normalized, excludeClientUserId);
    }

    public NicknameAvailabilityResponse checkAvailability(String nickname, Integer excludeClientUserId) {
        String normalized = normalizeRequired(nickname);
        return NicknameAvailabilityResponse.builder()
                .nickname(normalized)
                .available(isAvailable(normalized, excludeClientUserId))
                .build();
    }
}
