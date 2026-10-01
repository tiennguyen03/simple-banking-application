package com.bootcamp.banking.controllers;

import com.bootcamp.banking.repos.AccountRepository;
import com.bootcamp.banking.models.Account;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private final AccountRepository accounts;
    public DashboardController(AccountRepository accounts) { this.accounts = accounts; }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> admin() { return Map.of("message", "Administrator access granted"); }

    @GetMapping("/customerDashboard/{id}")
    @PreAuthorize("@accountAccess.customer(authentication, #id)")
    public List<Account> customerDashboard(@PathVariable String id) { return accounts.findByUserId(id); }
}

