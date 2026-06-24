package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.LoginRequest;
import com.arenagamer.api.dto.request.RegisterRequest;
import com.arenagamer.api.dto.response.AuthResponse;
import com.arenagamer.api.dto.response.UserPlanResponse;
import com.arenagamer.api.dto.response.UserResponse;
import com.arenagamer.api.entity.ArenaRefreshToken;
import com.arenagamer.api.entity.Client;
import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Staff;
import com.arenagamer.api.entity.Wallet;
import com.arenagamer.api.entity.enums.AuthUserType;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.ArenaRefreshTokenRepository;
import com.arenagamer.api.repository.ClientRankRepository;
import com.arenagamer.api.repository.ClientRepository;
import com.arenagamer.api.repository.ContactRepository;
import com.arenagamer.api.repository.StaffRepository;
import com.arenagamer.api.repository.WalletRepository;
import com.arenagamer.api.security.AuthenticatedUser;
import com.arenagamer.api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final StaffRepository staffRepository;
    private final ContactRepository contactRepository;
    private final ClientRepository clientRepository;
    private final ClientRankRepository clientRankRepository;
    private final WalletRepository walletRepository;
    private final ArenaRefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SubscriptionService subscriptionService;
    private final ClientNicknameService clientNicknameService;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (contactRepository.existsByEmail(request.getEmail()) || staffRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.conflict("Email já cadastrado");
        }

        String nickname = clientNicknameService.normalizeRequired(request.getNickname());
        clientNicknameService.ensureAvailable(nickname, null);

        Client client = Client.builder()
                .company(request.getFirstName() + " " + request.getLastName())
                .nickname(nickname)
                .phonenumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : "")
                .datecreated(LocalDateTime.now())
                .active(1)
                .build();
        client = clientRepository.save(client);

        Contact contact = Contact.builder()
                .userid(client.getUserId())
                .firstname(request.getFirstName())
                .lastname(request.getLastName())
                .email(request.getEmail())
                .phonenumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : "")
                .password(passwordEncoder.encode(request.getPassword()))
                .profileImage(normalizeUrl(request.getAvatarUrl()))
                .instagramUrl(normalizeUrl(request.getInstagramUrl()))
                .youtubeUrl(normalizeUrl(request.getYoutubeUrl()))
                .twitchUrl(normalizeUrl(request.getTwitchUrl()))
                .datecreated(LocalDateTime.now())
                .active(true)
                .isPrimary(1)
                .build();
        contact = contactRepository.save(contact);

        Wallet wallet = Wallet.builder().client(client).build();
        walletRepository.save(wallet);

        return buildAuthResponse(AuthenticatedUser.fromContact(contact));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        boolean asStaff = Boolean.TRUE.equals(request.getStaff());
        AuthenticatedUser user = asStaff ? loginStaff(request) : loginContact(request);
        return buildAuthResponse(user);
    }

    private AuthenticatedUser loginStaff(LoginRequest request) {
        Staff staff = staffRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> BusinessException.unauthorized("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), staff.getPassword())) {
            throw BusinessException.unauthorized("Credenciais inválidas");
        }
        if (staff.getActive() == null || staff.getActive() != 1) {
            throw BusinessException.forbidden("Conta desativada");
        }
        return AuthenticatedUser.fromStaff(staff);
    }

    private AuthenticatedUser loginContact(LoginRequest request) {
        Contact contact = contactRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> BusinessException.unauthorized("Credenciais inválidas"));

        if (contact.getPassword() == null || !passwordEncoder.matches(request.getPassword(), contact.getPassword())) {
            throw BusinessException.unauthorized("Credenciais inválidas");
        }
        if (contact.getActive() == null || !contact.getActive()) {
            throw BusinessException.forbidden("Conta desativada");
        }
        return AuthenticatedUser.fromContact(contact);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw BusinessException.unauthorized("Refresh token inválido ou expirado");
        }
        if (!"refresh".equals(jwtUtil.extractTokenType(refreshToken))) {
            throw BusinessException.unauthorized("Token tipo inválido");
        }

        ArenaRefreshToken stored = refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> BusinessException.unauthorized("Refresh token não reconhecido"));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw BusinessException.unauthorized("Refresh token expirado");
        }

        AuthenticatedUser user = resolve(stored.getUserType(), stored.getUserId());
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(AuthUserType userType, Long userId) {
        refreshTokenRepository.deleteByUserTypeAndUserId(userType, userId);
    }

    private AuthenticatedUser resolve(AuthUserType type, Long id) {
        if (type == AuthUserType.STAFF) {
            Staff staff = staffRepository.findById(id.intValue())
                    .orElseThrow(() -> BusinessException.unauthorized("Usuário não encontrado"));
            return AuthenticatedUser.fromStaff(staff);
        }
        Contact contact = contactRepository.findById(id.intValue())
                .orElseThrow(() -> BusinessException.unauthorized("Usuário não encontrado"));
        return AuthenticatedUser.fromContact(contact);
    }

    private AuthResponse buildAuthResponse(AuthenticatedUser user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpiration());

        ArenaRefreshToken stored = refreshTokenRepository
                .findByUserTypeAndUserId(user.getType(), user.getId())
                .orElseGet(() -> ArenaRefreshToken.builder()
                        .userType(user.getType())
                        .userId(user.getId())
                        .build());
        stored.setRefreshToken(refreshToken);
        stored.setExpiresAt(expiresAt);
        refreshTokenRepository.save(stored);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration)
                .user(buildUserResponse(user))
                .build();
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
                    .map(rank -> com.arenagamer.api.dto.response.TeamRankSummaryResponse.builder()
                            .presetId(rank.getPreset().getId())
                            .gameName(rank.getPreset().getGameName())
                            .platform(rank.getPreset().getPlatform())
                            .rankPoints(rank.getRankPoints())
                            .build())
                    .toList());
        }
        return response;
    }

    private String normalizeUrl(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
