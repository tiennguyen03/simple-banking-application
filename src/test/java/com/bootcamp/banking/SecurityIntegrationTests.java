package com.bootcamp.banking;

import com.bootcamp.banking.config.*;
import com.bootcamp.banking.controllers.*;
import com.bootcamp.banking.models.*;
import com.bootcamp.banking.repos.*;
import com.bootcamp.banking.services.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(SecurityIntegrationTests.TestConfig.class)
@WebAppConfiguration
@TestPropertySource(properties = "banking.auth.jwt-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class SecurityIntegrationTests {
    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, JwtConfig.class, PasswordConfig.class, AccountAccess.class,
        AuthService.class, AuthController.class, AccountController.class, CustomerController.class,
        AuditController.class, DashboardController.class})
    static class TestConfig {
        @Bean UserCredentialRepository credentials() { return mock(UserCredentialRepository.class); }
        @Bean CustomerRepository customers() { return mock(CustomerRepository.class); }
        @Bean AccountRepository accounts() { return mock(AccountRepository.class); }
        @Bean AccountService accountService() { return mock(AccountService.class); }
        @Bean CustomerService customerService() { return mock(CustomerService.class); }
        @Bean AuditService auditService() { return mock(AuditService.class); }
    }

    @Autowired WebApplicationContext context;
    @Autowired UserCredentialRepository credentials;
    @Autowired CustomerRepository customers;
    @Autowired AccountRepository accounts;
    @Autowired AccountService accountService;
    @Autowired CustomerService customerService;
    @Autowired AuditService auditService;
    @Autowired PasswordEncoder passwords;
    @Autowired AuthService auth;
    @Autowired JwtEncoder encoder;
    MockMvc mvc;
    UserCredential customer;
    UserCredential admin;
    String customerToken;
    String adminToken;

    @BeforeEach
    void setup() {
        reset(credentials, customers, accounts, accountService, customerService, auditService);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        customer = new UserCredential("customer", passwords.encode("test-password-only"), UserRole.CUSTOMER, "customer-1");
        customer.setId("credential-1");
        admin = new UserCredential("admin", passwords.encode("test-password-only"), UserRole.ADMIN, null);
        admin.setId("credential-2");
        for (var user : List.of(customer, admin)) {
            when(credentials.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
            when(credentials.findById(user.getId())).thenReturn(Optional.of(user));
        }
        when(customers.existsById("customer-1")).thenReturn(true);
        var own = new Account("account-1", "customer-1", "SAVINGS", BigDecimal.TEN);
        var other = new Account("account-2", "customer-2", "SAVINGS", BigDecimal.TEN);
        when(accounts.findById("account-1")).thenReturn(Optional.of(own));
        when(accounts.findById("account-2")).thenReturn(Optional.of(other));
        when(accountService.getAccountById("account-1")).thenReturn(own);
        when(accountService.getAccountsByUserId("customer-1")).thenReturn(List.of(own));
        customerToken = auth.login(new com.bootcamp.banking.requests.LoginRequest("customer", "test-password-only")).token();
        adminToken = auth.login(new com.bootcamp.banking.requests.LoginRequest("admin", "test-password-only")).token();
    }

    @Test void sharedLoginAcceptsBothRolesAndRejectsBadPasswords() throws Exception {
        for (String username : List.of("customer", "admin")) {
            mvc.perform(post("/api/auth/login").contentType("application/json").content(
                "{\"username\":\"" + username + "\",\"password\":\"test-password-only\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        }
        for (String username : List.of("customer", "unknown")) {
            mvc.perform(post("/api/auth/login").contentType("application/json").content(
                "{\"username\":\"" + username + "\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Test void anonymousInvalidExpiredAndDeletedUsersAreRejected() throws Exception {
        mvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/accounts").header("Authorization", "Bearer broken-token")).andExpect(status().isUnauthorized());
        Instant now = Instant.now();
        String expired = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
            JwtClaimsSet.builder().issuer("simple-bank").subject(customer.getId()).issuedAt(now.minusSeconds(600))
                .expiresAt(now.minusSeconds(120)).build())).getTokenValue();
        mvc.perform(get("/api/accounts").header("Authorization", "Bearer " + expired)).andExpect(status().isUnauthorized());
        when(credentials.findById(customer.getId())).thenReturn(Optional.empty());
        mvc.perform(get("/api/accounts").header("Authorization", "Bearer " + customerToken)).andExpect(status().isUnauthorized());
    }

    @Test void customerReadsOnlyOwnAccountsAndHistory() throws Exception {
        mvc.perform(get("/api/accounts").header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].userId").value("customer-1"));
        mvc.perform(get("/api/accounts/account-1/transactions").header("Authorization", "Bearer " + customerToken)).andExpect(status().isOk());
        for (String path : List.of("/api/accounts/account-2", "/api/accounts/account-2/transactions",
                "/api/accounts?userId=customer-2", "/api/customerDashboard/customer-2", "/api/customers/customer-2")) {
            mvc.perform(get(path).header("Authorization", "Bearer " + customerToken)).andExpect(status().isForbidden());
        }
    }

    @Test void customerCannotUseAdminResourcesOrTransferOthersMoney() throws Exception {
        for (String path : List.of("/api/admin", "/api/customers", "/api/audits", "/api/accounts/premium?threshold=100")) {
            mvc.perform(get(path).header("Authorization", "Bearer " + customerToken)).andExpect(status().isForbidden());
            mvc.perform(get(path).header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
        }
        mvc.perform(post("/api/accounts/account-1/deposit").header("Authorization", "Bearer " + customerToken)
            .contentType("application/json").content("{\"amount\":10}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/accounts/transfer").header("Authorization", "Bearer " + customerToken)
            .contentType("application/json").content("{\"fromAccountId\":\"account-2\",\"toAccountId\":\"account-1\",\"amount\":1}"))
            .andExpect(status().isForbidden());
        verify(accountService, never()).transfer(any(), any(), any());
    }

    @Test void registrationAlwaysCreatesCustomerAndHashesPassword() throws Exception {
        when(customers.save(any())).thenReturn(new Customer("new-customer", "New Customer"));
        when(credentials.insert(any(UserCredential.class))).thenAnswer(invocation -> {
            UserCredential value = invocation.getArgument(0);
            org.junit.jupiter.api.Assertions.assertEquals(UserRole.CUSTOMER, value.getRole());
            org.junit.jupiter.api.Assertions.assertEquals("new-customer", value.getCustomerId());
            org.junit.jupiter.api.Assertions.assertTrue(passwords.matches("test-password-only", value.getPasswordHash()));
            value.setId("new-credential");
            return value;
        });
        mvc.perform(post("/api/auth/register").contentType("application/json").content(
            "{\"username\":\"new-user\",\"password\":\"test-password-only\",\"name\":\"New Customer\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("CUSTOMER"));
        when(credentials.existsByUsername("new-user")).thenReturn(true);
        mvc.perform(post("/api/auth/register").contentType("application/json").content(
            "{\"username\":\"new-user\",\"password\":\"test-password-only\",\"name\":\"New Customer\"}"))
            .andExpect(status().isConflict());
    }
}
