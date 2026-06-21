package com.arenagamer.api.controller.common;

import com.arenagamer.api.dto.request.UpdateProfileRequest;
import com.arenagamer.api.dto.response.ApiMessages;
import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.UserPlanResponse;
import com.arenagamer.api.dto.response.UserResponse;
import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Staff;
import com.arenagamer.api.entity.enums.AuthUserType;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.ContactRepository;
import com.arenagamer.api.repository.StaffRepository;
import com.arenagamer.api.security.AuthenticatedUser;
import com.arenagamer.api.security.UserPrincipal;
import com.arenagamer.api.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/common/users")
@RequiredArgsConstructor
@Tag(name = "Common / Usuário", description = "Perfil do usuário logado — JWT (staff ou cliente)")
@SecurityRequirement(name = "Bearer")
public class CommonUserController {

    private final StaffRepository staffRepository;
    private final ContactRepository contactRepository;
    private final SubscriptionService subscriptionService;

    @GetMapping("/me")
    @Operation(summary = "Obter perfil")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        AuthenticatedUser auth = UserPrincipal.current();
        return ApiResponses.fetched(buildUserResponse(auth));
    }

    @PutMapping("/me")
    @Operation(summary = "Atualizar perfil")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String avatarUrl,
            @RequestParam(required = false) String instagramUrl,
            @RequestParam(required = false) String youtubeUrl,
            @RequestParam(required = false) String twitchUrl,
            @Valid @RequestBody(required = false) UpdateProfileRequest body) {

        UpdateProfileRequest request = mergeProfileRequest(
                firstName, lastName, phoneNumber, avatarUrl, instagramUrl, youtubeUrl, twitchUrl, body);

        AuthenticatedUser auth = UserPrincipal.current();

        if (auth.getType() == AuthUserType.STAFF) {
            Staff staff = staffRepository.findById(auth.getId().intValue())
                    .orElseThrow(() -> BusinessException.notFound("Usuário não encontrado"));
            if (request.getFirstName() != null) {
                staff.setFirstname(request.getFirstName());
            }
            if (request.getLastName() != null) {
                staff.setLastname(request.getLastName());
            }
            if (request.getPhoneNumber() != null) {
                staff.setPhonenumber(request.getPhoneNumber());
            }
            if (request.getAvatarUrl() != null) {
                staff.setProfileImage(normalizeUrl(request.getAvatarUrl()));
            }
            staffRepository.save(staff);
            return ApiResponses.updated(ApiMessages.PROFILE_UPDATED, buildUserResponse(AuthenticatedUser.fromStaff(staff)));
        }

        Contact contact = contactRepository.findById(auth.getId().intValue())
                .orElseThrow(() -> BusinessException.notFound("Usuário não encontrado"));
        if (request.getFirstName() != null) {
            contact.setFirstname(request.getFirstName());
        }
        if (request.getLastName() != null) {
            contact.setLastname(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            contact.setPhonenumber(request.getPhoneNumber());
        }
        if (request.getAvatarUrl() != null) {
            contact.setProfileImage(normalizeUrl(request.getAvatarUrl()));
        }
        if (request.getInstagramUrl() != null) {
            contact.setInstagramUrl(normalizeUrl(request.getInstagramUrl()));
        }
        if (request.getYoutubeUrl() != null) {
            contact.setYoutubeUrl(normalizeUrl(request.getYoutubeUrl()));
        }
        if (request.getTwitchUrl() != null) {
            contact.setTwitchUrl(normalizeUrl(request.getTwitchUrl()));
        }
        contactRepository.save(contact);
        return ApiResponses.updated(ApiMessages.PROFILE_UPDATED, buildUserResponse(AuthenticatedUser.fromContact(contact)));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Desativar conta")
    public ResponseEntity<ApiResponse<Void>> deactivate() {
        AuthenticatedUser auth = UserPrincipal.current();

        if (auth.getType() == AuthUserType.STAFF) {
            Staff staff = staffRepository.findById(auth.getId().intValue())
                    .orElseThrow(() -> BusinessException.notFound("Usuário não encontrado"));
            staff.setActive(0);
            staffRepository.save(staff);
        } else {
            Contact contact = contactRepository.findById(auth.getId().intValue())
                    .orElseThrow(() -> BusinessException.notFound("Usuário não encontrado"));
            contact.setActive(false);
            contactRepository.save(contact);
        }

        return ApiResponses.okMessage(ApiMessages.ACCOUNT_DEACTIVATED);
    }

    private UpdateProfileRequest mergeProfileRequest(
            String firstName,
            String lastName,
            String phoneNumber,
            String avatarUrl,
            String instagramUrl,
            String youtubeUrl,
            String twitchUrl,
            UpdateProfileRequest body) {
        UpdateProfileRequest request = body != null ? body : new UpdateProfileRequest();
        if (firstName != null) {
            request.setFirstName(firstName);
        }
        if (lastName != null) {
            request.setLastName(lastName);
        }
        if (phoneNumber != null) {
            request.setPhoneNumber(phoneNumber);
        }
        if (avatarUrl != null) {
            request.setAvatarUrl(avatarUrl);
        }
        if (instagramUrl != null) {
            request.setInstagramUrl(instagramUrl);
        }
        if (youtubeUrl != null) {
            request.setYoutubeUrl(youtubeUrl);
        }
        if (twitchUrl != null) {
            request.setTwitchUrl(twitchUrl);
        }
        return request;
    }

    private String normalizeUrl(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UserResponse buildUserResponse(AuthenticatedUser user) {
        UserPlanResponse plan = user.isContact()
                ? subscriptionService.getActivePlanForClient(user.getClientUserId())
                : null;
        return UserResponse.from(user, plan);
    }
}
