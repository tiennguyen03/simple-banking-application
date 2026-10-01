package com.bootcamp.banking.config;

import com.bootcamp.banking.models.UserRole;
import com.bootcamp.banking.repos.UserCredentialRepository;
import com.bootcamp.banking.repos.CustomerRepository;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            UserCredentialRepository credentials, CustomerRepository customers) throws Exception {
        // Tokens are sent in the Authorization header, never in automatically sent cookies.
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/api/admin", "/api/admin/**", "/api/audits", "/api/audits/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll())
            .oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.jwtAuthenticationConverter(token -> {
                var user = credentials.findById(token.getSubject()).orElseThrow(() ->
                    new OAuth2AuthenticationException(new OAuth2Error("invalid_token")));
                if (user.getRole() == null || (user.getRole() == UserRole.CUSTOMER &&
                        (user.getCustomerId() == null || !customers.existsById(user.getCustomerId())))) {
                    throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"));
                }
                // Read current permissions from MongoDB; deleted credentials cannot keep using a JWT.
                return new JwtAuthenticationToken(token,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())), user.getUsername());
            })));
        return http.build();
    }
}
