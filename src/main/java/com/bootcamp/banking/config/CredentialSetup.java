package com.bootcamp.banking.config;

import com.bootcamp.banking.models.*;
import com.bootcamp.banking.repos.UserCredentialRepository;
import com.bootcamp.banking.services.AuthService;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class CredentialSetup {
    @Bean
    ApplicationRunner setupCredentials(MongoTemplate mongo, UserCredentialRepository credentials,
            PasswordEncoder passwords, @Value("${BANK_ADMIN_USERNAME:}") String username,
            @Value("${BANK_ADMIN_PASSWORD:}") String password) {
        return args -> {
            // Enforce uniqueness in MongoDB, including concurrent registrations.
            mongo.indexOps(UserCredential.class).ensureIndex(new Index().on("username", Sort.Direction.ASC).unique());
            if (username.isBlank() && password.isBlank()) return;
            if (!username.matches("[A-Za-z0-9._-]{3,80}") || password.length() < 10 ||
                    password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException("Set valid BANK_ADMIN_USERNAME and BANK_ADMIN_PASSWORD together");
            }
            String normalized = AuthService.normalizeUsername(username);
            var existing = credentials.findByUsername(normalized);
            if (existing.isPresent()) {
                if (existing.get().getRole() != UserRole.ADMIN)
                    throw new IllegalStateException("Administrator username belongs to a customer");
                return; // Never silently change an existing administrator's password.
            }
            credentials.insert(new UserCredential(normalized, passwords.encode(password), UserRole.ADMIN, null));
        };
    }
}

