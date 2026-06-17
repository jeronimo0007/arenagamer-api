package com.arenagamer.api.controller;

import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.UserResponse;
import com.arenagamer.api.entity.User;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.UserRepository;
import com.arenagamer.api.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gerenciamento de usuários")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    @Operation(summary = "Obter perfil do usuário logado")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        User user = UserPrincipal.current();
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(user)));
    }

    @PutMapping("/me")
    @Operation(summary = "Atualizar perfil")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String timezone) {

        User user = userRepository.findById(UserPrincipal.currentId())
                .orElseThrow(() -> BusinessException.notFound("Usuário não encontrado"));

        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (phoneNumber != null) user.setPhoneNumber(phoneNumber);
        if (timezone != null) user.setTimezone(timezone);
        if (username != null) {
            if (userRepository.existsByUsername(username) && !username.equals(user.getUsername())) {
                throw BusinessException.conflict("Username já em uso");
            }
            user.setUsername(username);
        }

        user = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(user)));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Desativar conta")
    public ResponseEntity<ApiResponse<Void>> deactivate() {
        User user = userRepository.findById(UserPrincipal.currentId())
                .orElseThrow(() -> BusinessException.notFound("Usuário não encontrado"));
        user.setActive(false);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("Conta desativada"));
    }
}
