package com.bootcamp.banking.controllers;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import com.bootcamp.banking.services.AccountAccess;

import com.bootcamp.banking.models.Account;
import com.bootcamp.banking.models.Transaction;
import com.bootcamp.banking.requests.AmountRequest;
import com.bootcamp.banking.requests.TransferRequest;
import com.bootcamp.banking.responses.TransferResponse;
import com.bootcamp.banking.services.AccountService;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountAccess accountAccess;

    public AccountController(AccountService accountService, AccountAccess accountAccess) {
        this.accountService = accountService;
        this.accountAccess = accountAccess;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Account> createAccount(
            @RequestBody Account accountRequest) {

        try {
            Account createdAccount =
                    accountService.createAccount(accountRequest);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(createdAccount);
        } catch (NoSuchElementException exception) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    public ResponseEntity<List<Account>> getAccounts(
            @RequestParam(required = false) String userId) {

        if (!accountAccess.currentIsAdmin()) {
            String customerId = accountAccess.currentCustomerId();
            if (customerId == null || (userId != null && !userId.isBlank() && !userId.equals(customerId))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(accountService.getAccountsByUserId(customerId));
        }
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.ok(accountService.getAllAccounts());
        }

        return ResponseEntity.ok(
                accountService.getAccountsByUserId(userId));
    }

    @GetMapping("/premium")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Account>> getPremiumAccounts(
            @RequestParam BigDecimal threshold) {
        try {
            return ResponseEntity.ok(
                    accountService.getPremiumAccounts(threshold));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/transfer")
    @PreAuthorize("@accountAccess.account(authentication, #request.fromAccountId) and @accountAccess.account(authentication, #request.toAccountId)")
    public ResponseEntity<TransferResponse> transfer(
            @RequestBody TransferRequest request) {
        try {
            return ResponseEntity.ok(accountService.transfer(
                    request.getFromAccountId(), request.getToAccountId(),
                    request.getAmount()));
        } catch (NoSuchElementException exception) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("@accountAccess.account(authentication, #id)")
    public ResponseEntity<Account> getAccountById(
            @PathVariable String id) {

        Account account = accountService.getAccountById(id);

        if (account == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(account);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Account> updateAccount(
            @PathVariable String id,
            @RequestBody Account accountRequest) {

        try {
            Account account =
                    accountService.updateAccount(id, accountRequest);

            if (account == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(account);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAccount(@PathVariable String id) {
        if (!accountService.deleteAccount(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deposit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Account> deposit(
            @PathVariable String id,
            @RequestBody AmountRequest request) {

        try {
            Account account =
                    accountService.deposit(id, request.getAmount());

            if (account == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(account);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Account> withdraw(
            @PathVariable String id,
            @RequestBody AmountRequest request) {

        try {
            Account account =
                    accountService.withdraw(id, request.getAmount());

            if (account == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(account);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/transactions")
    @PreAuthorize("@accountAccess.account(authentication, #id)")
    public ResponseEntity<List<Transaction>> getTransactions(
            @PathVariable String id) {

        if (accountService.getAccountById(id) == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                accountService.getTransactions(id));
    }
}
