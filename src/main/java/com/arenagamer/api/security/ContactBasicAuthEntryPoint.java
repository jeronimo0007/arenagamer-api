package com.arenagamer.api.security;

import com.arenagamer.api.dto.response.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ContactBasicAuthEntryPoint implements AuthenticationEntryPoint {

    private static final String REALM = "Basic realm=\"ArenaGamer\"";

    private final ApiErrorWriter apiErrorWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, REALM);
        apiErrorWriter.write(response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED,
                "Autenticação Basic necessária. Use email e senha de staff ou contact.");
    }
}
