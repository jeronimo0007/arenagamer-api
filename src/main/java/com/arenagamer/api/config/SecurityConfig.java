package com.arenagamer.api.config;

import com.arenagamer.api.security.ContactBasicAuthEnrichmentFilter;
import com.arenagamer.api.security.ContactBasicAuthEntryPoint;
import com.arenagamer.api.security.ContactUserDetailsService;
import com.arenagamer.api.security.JwtAuthenticationFilter;
import com.arenagamer.api.security.PerfexPasswordEncoder;
import com.arenagamer.api.security.PublicBasicAuthRequestMatcher;
import com.arenagamer.api.security.RestAccessDeniedHandler;
import com.arenagamer.api.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final ContactUserDetailsService contactUserDetailsService;
    private final ContactBasicAuthEntryPoint contactBasicAuthEntryPoint;
    private final PublicBasicAuthRequestMatcher publicBasicAuthRequestMatcher;
    private final ContactBasicAuthEnrichmentFilter contactBasicAuthEnrichmentFilter;
    private final PerfexPasswordEncoder perfexPasswordEncoder;

    @Bean
    @Order(1)
    public SecurityFilterChain publicBasicAuthFilterChain(HttpSecurity http) throws Exception {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(contactUserDetailsService);
        authProvider.setPasswordEncoder(perfexPasswordEncoder);

        http
            .securityMatcher(publicBasicAuthRequestMatcher)
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authProvider)
            .authorizeHttpRequests(authz -> authz.anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults())
            .addFilterAfter(contactBasicAuthEnrichmentFilter, BasicAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(contactBasicAuthEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain jwtFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/v1/public/auth/**").permitAll()
                .requestMatchers("/api/v1/oauth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/v1/common/**").authenticated()
                .anyRequest().denyAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(PerfexPasswordEncoder perfexPasswordEncoder) {
        return perfexPasswordEncoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
