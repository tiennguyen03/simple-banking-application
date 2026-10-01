package com.bootcamp.banking.services;

import com.bootcamp.banking.models.UserRole;
import com.bootcamp.banking.repos.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("accountAccess")
public class AccountAccess {
    private final AccountRepository accounts;
    private final UserCredentialRepository credentials;
    public AccountAccess(AccountRepository accounts, UserCredentialRepository credentials) {
        this.accounts = accounts;
        this.credentials = credentials;
    }

    public boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public String customerId(Authentication auth) {
        if (!(auth instanceof JwtAuthenticationToken jwt)) return null;
        return credentials.findById(jwt.getToken().getSubject())
            .filter(user -> user.getRole() == UserRole.CUSTOMER)
            .map(user -> user.getCustomerId()).orElse(null);
    }

    public boolean customer(Authentication auth, String id) {
        if (isAdmin(auth)) return true;
        String ownId = customerId(auth);
        return ownId != null && ownId.equals(id);
    }

    public boolean account(Authentication auth, String id) {
        if (isAdmin(auth)) return true;
        if (id == null) return false;
        return accounts.findById(id).map(account -> customer(auth, account.getUserId())).orElse(false);
    }

    public String currentCustomerId() {
        return customerId(SecurityContextHolder.getContext().getAuthentication());
    }

    public boolean currentIsAdmin() {
        return isAdmin(SecurityContextHolder.getContext().getAuthentication());
    }
}

