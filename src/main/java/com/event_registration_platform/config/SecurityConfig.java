
package com.event_registration_platform.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                // Return 401 when authentication is missing
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                response.setStatus(
                                        HttpServletResponse.SC_UNAUTHORIZED
                                )
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Swagger APIs
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Authentication APIs
                        .requestMatchers("/api/auth/**").permitAll()

                        // User profile APIs
                        .requestMatchers("/api/users/**").authenticated()

                        // Organiser's events
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/events/my"
                        ).authenticated()

                        // Public event APIs
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/events",
                                "/api/events/*"
                        ).permitAll()

                        // All remaining APIs require authentication
                        .anyRequest().authenticated()
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
