package com.arenagamer.api.security;

import com.arenagamer.api.repository.ContactRepository;
import com.arenagamer.api.repository.StaffRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Converte o principal do HTTP Basic ({@link User}) em {@link AuthenticatedUser}
 * para endpoints que usam UserPrincipal (staff ou contact).
 */
@Component
@RequiredArgsConstructor
public class ContactBasicAuthEnrichmentFilter extends OncePerRequestFilter {

    private final StaffRepository staffRepository;
    private final ContactRepository contactRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof User user
                && !(authentication.getPrincipal() instanceof AuthenticatedUser)) {
            enrichAuthentication(user.getUsername(), authentication);
        }

        filterChain.doFilter(request, response);
    }

    private void enrichAuthentication(String email, org.springframework.security.core.Authentication authentication) {
        staffRepository.findByEmail(email).ifPresentOrElse(staff -> {
            AuthenticatedUser authenticatedUser = AuthenticatedUser.fromStaff(staff);
            setAuthenticatedUser(authentication, authenticatedUser);
        }, () -> contactRepository.findByEmail(email).ifPresent(contact -> {
            AuthenticatedUser authenticatedUser = AuthenticatedUser.fromContact(contact);
            setAuthenticatedUser(authentication, authenticatedUser);
        }));
    }

    private void setAuthenticatedUser(org.springframework.security.core.Authentication authentication,
                                      AuthenticatedUser authenticatedUser) {
        var enriched = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                authentication.getCredentials(),
                authentication.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(enriched);
    }
}
