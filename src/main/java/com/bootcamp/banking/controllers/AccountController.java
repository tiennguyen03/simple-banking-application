package com.bootcamp.banking.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.bootcamp.banking.models.Account;
import com.bootcamp.banking.services.AccountService;
import java.util.List;
import com.bootcamp.banking.models.Transaction;
import com.bootcamp.banking.requests.AmountRequest;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(
            @RequestBody Account accountRequest) {

        Account createdAccount =
                accountService.createAccount(accountRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdAccount);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccountById(
            @PathVariable String id) {

        Account account = accountService.getAccountById(id);

        if (account == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(account);
    }

    @PostMapping("/{id}/deposit")
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
    public ResponseEntity<List<Transaction>> getTransactions(
            @PathVariable String id) {

        if (accountService.getAccountById(id) == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                accountService.getTransactions(id));
    }
}
