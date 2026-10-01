package com.bootcamp.banking.services;

import com.bootcamp.banking.models.*;
import com.bootcamp.banking.repos.*;
import com.bootcamp.banking.requests.*;
import com.bootcamp.banking.responses.LoginResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserCredentialRepository credentials;
    private final CustomerRepository customers;
    private final PasswordEncoder passwords;
    private final JwtEncoder tokens;
    private final String dummyHash;

    public AuthService(UserCredentialRepository credentials, CustomerRepository customers,
                       PasswordEncoder passwords, JwtEncoder tokens) {
        this.credentials = credentials;
        this.customers = customers;
        this.passwords = passwords;
        this.tokens = tokens;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    public static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    public LoginResponse login(LoginRequest request) {
        var user = credentials.findByUsername(normalizeUsername(request.username())).orElse(null);
        // Always perform a BCrypt comparison, including when the username is unknown.
        boolean matches = request.password().getBytes(StandardCharsets.UTF_8).length <= 72 &&
            passwords.matches(request.password(), user == null ? dummyHash : user.getPasswordHash());
        if (!matches || user == null || user.getRole() == null ||
                (user.getRole() == UserRole.CUSTOMER &&
                 (user.getCustomerId() == null || !customers.existsById(user.getCustomerId())))) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return issueToken(user);
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
        }
        String username = normalizeUsername(request.username());
        if (credentials.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is unavailable");
        }
        Customer customer = customers.save(new Customer(null, request.name().trim()));
        try {
            var user = credentials.insert(new UserCredential(username, passwords.encode(request.password()),
                UserRole.CUSTOMER, customer.getId()));
            return issueToken(user);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is unavailable");
        }
    }

    private LoginResponse issueToken(UserCredential user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("simple-bank").subject(user.getId())
            .issuedAt(now).expiresAt(now.plusSeconds(900)).id(UUID.randomUUID().toString()).build();
        String token = tokens.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", 900, user.getUsername(), user.getRole(), user.getCustomerId());
    }
}

