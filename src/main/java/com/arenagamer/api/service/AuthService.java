package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.LoginRequest;
import com.arenagamer.api.dto.request.RegisterRequest;
import com.arenagamer.api.dto.response.AuthResponse;
import com.arenagamer.api.dto.response.UserResponse;
import com.arenagamer.api.entity.User;
import com.arenagamer.api.entity.Wallet;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.UserRepository;
import com.arenagamer.api.repository.WalletRepository;
import com.arenagamer.api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.conflict("Email já cadastrado");
        }
        if (request.getUsername() != null && userRepository.existsByUsername(request.getUsername())) {
            throw BusinessException.conflict("Username já está em uso");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .username(request.getUsername())
                .phoneNumber(request.getPhoneNumber())
                .timezone(request.getTimezone())
                .build();

        user = userRepository.save(user);

        Wallet wallet = Wallet.builder().user(user).build();
        walletRepository.save(wallet);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> BusinessException.unauthorized("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw BusinessException.unauthorized("Credenciais inválidas");
        }

        if (!user.getActive()) {
            throw BusinessException.forbidden("Conta desativada");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw BusinessException.unauthorized("Refresh token inválido ou expirado");
        }
        if (!"refresh".equals(jwtUtil.extractTokenType(refreshToken))) {
            throw BusinessException.unauthorized("Token tipo inválido");
        }

        Long userId = jwtUtil.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.unauthorized("Usuário não encontrado"));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw BusinessException.unauthorized("Refresh token não reconhecido");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setRefreshToken(null);
            userRepository.save(user);
        });
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration)
                .user(UserResponse.from(user))
                .build();
    }
}
