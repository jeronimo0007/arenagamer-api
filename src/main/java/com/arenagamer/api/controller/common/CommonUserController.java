package com.arenagamer.api.controller.common;

import com.arenagamer.api.dto.request.UpdateProfileRequest;
import com.arenagamer.api.dto.response.ApiMessages;
import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.NicknameAvailabilityResponse;
import com.arenagamer.api.dto.response.TeamRankSummaryResponse;
import com.arenagamer.api.dto.response.UserPlanResponse;
import com.arenagamer.api.dto.response.UserResponse;
import com.arenagamer.api.entity.Client;
import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Staff;
import com.arenagamer.api.entity.enums.AuthUserType;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.ClientRankRepository;
import com.arenagamer.api.repository.ClientRepository;
import com.arenagamer.api.repository.ContactRepository;
import com.arenagamer.api.repository.StaffRepository;
import com.arenagamer.api.security.AuthenticatedUser;
import com.arenagamer.api.security.UserPrincipal;
import com.arenagamer.api.service.AvailabilityService;
import com.arenagamer.api.service.ClientNicknameService;
import com.arenagamer.api.service.ClientRankService;
import com.arenagamer.api.service.SubscriptionService;
import com.arenagamer.api.util.NicknameRules;
import com.arenagamer.api.entity.enums.Visibility;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/common/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "Common / Usuário", description = "Perfil do usuário logado — JWT (staff ou cliente)")
@SecurityRequirement(name = "Bearer")
public class CommonUserController {

    private final StaffRepository staffRepository;
    private final ContactRepository contactRepository;
    private final ClientRepository clientRepository;
    private final ClientRankRepository clientRankRepository;
    private final ClientRankService clientRankService;
    private final ClientNicknameService clientNicknameService;
    private final AvailabilityService availabilityService;
    private final SubscriptionService subscriptionService;

    @GetMapping("/nickname-available")
    @Operation(
            summary = "Verificar se nickname está disponível",
            description = "Letras e números apenas. Ignora o nickname do cliente logado.")
    public ResponseEntity<ApiResponse<NicknameAvailabilityResponse>> checkNickname(
            @RequestParam
            @NotBlank
            @Size(max = 50)
            @Pattern(regexp = NicknameRules.REGEX, message = NicknameRules.VALIDATION_MESSAGE)
            String nickname) {
        AuthenticatedUser auth = UserPrincipal.current();
        Integer excludeClientUserId = auth.isContact() ? auth.getClientUserId() : null;
        return ApiResponses.fetched(clientNicknameService.checkAvailability(nickname, excludeClientUserId));
    }

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
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) Visibility visibility,
            @Valid @RequestBody(required = false) UpdateProfileRequest body) {

        UpdateProfileRequest request = mergeProfileRequest(
                firstName, lastName, phoneNumber, avatarUrl, instagramUrl, youtubeUrl, twitchUrl,
                nickname, visibility, body);

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

        if (Integer.valueOf(1).equals(contact.getIsPrimary())) {
            updateClientProfile(contact.getUserid(), request);
        }

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
            String nickname,
            Visibility visibility,
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
        if (nickname != null) {
            request.setNickname(nickname);
        }
        if (visibility != null) {
            request.setVisibility(visibility);
        }
        return request;
    }

    private void updateClientProfile(Integer clientUserId, UpdateProfileRequest request) {
        Client client = clientRepository.findById(clientUserId)
                .orElseThrow(() -> BusinessException.notFound("Cliente não encontrado"));
        if (request.getNickname() != null) {
            String nickname = clientNicknameService.normalizeRequired(request.getNickname());
            clientNicknameService.ensureAvailable(nickname, clientUserId);
            client.setNickname(nickname);
        }
        if (request.getVisibility() != null) {
            client.setVisibility(request.getVisibility());
        }
        clientRepository.save(client);
        if (request.getRanks() != null) {
            clientRankService.syncRanks(client, request.getRanks());
        }
        if (request.getAvailability() != null && request.getAvailability().getWeeklySlots() != null) {
            Contact primaryContact = contactRepository.findByUseridAndIsPrimary(clientUserId, 1)
                    .orElseThrow(() -> BusinessException.notFound("Contato primário não encontrado"));
            availabilityService.syncClientSchedule(
                    clientUserId, primaryContact, request.getAvailability().getWeeklySlots());
        }
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
        UserResponse response = UserResponse.from(user, plan);
        if (user.isContact() && user.getClientUserId() != null) {
            clientRepository.findById(user.getClientUserId()).ifPresent(client -> {
                response.setNickname(client.getNickname());
                response.setPrivacy(client.getVisibility());
            });
            response.setRanks(clientRankRepository.findByClientUserIdWithPreset(user.getClientUserId()).stream()
                    .map(rank -> TeamRankSummaryResponse.builder()
                            .presetId(rank.getPreset().getId())
                            .gameName(rank.getPreset().getGameName())
                            .platform(rank.getPreset().getPlatform())
                            .rankPoints(rank.getRankPoints())
                            .build())
                    .toList());
            response.setAvailability(availabilityService.getClientSchedule(user.getClientUserId()));
        }
        return response;
    }
}
