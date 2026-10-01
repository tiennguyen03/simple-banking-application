package com.bootcamp.banking.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.json.JsonMapper;
import com.bootcamp.banking.models.UserCredential;
import com.bootcamp.banking.models.UserRole;
import static org.junit.jupiter.api.Assertions.*;

class PasswordConfigTests {
    @Test
    void passwordsAreSaltedAndOnlyTheCorrectPasswordMatches() {
        PasswordEncoder encoder = new PasswordConfig().passwordEncoder();
        String first = encoder.encode("test-password-only");
        String second = encoder.encode("test-password-only");
        assertNotEquals("test-password-only", first);
        assertNotEquals(first, second);
        assertTrue(encoder.matches("test-password-only", first));
        assertFalse(encoder.matches("incorrect-password", first));
    }

    @Test
    void credentialJsonDoesNotExposePasswordHash() {
        UserCredential credential = new UserCredential("test-user", "private-hash", UserRole.CUSTOMER, "customer-1");
        String json = JsonMapper.builder().build().writeValueAsString(credential);
        assertFalse(json.contains("passwordHash"));
        assertFalse(json.contains("private-hash"));
        assertTrue(json.contains("customer-1"));
    }
}
