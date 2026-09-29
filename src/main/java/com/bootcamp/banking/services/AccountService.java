package com.bootcamp.banking.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.bootcamp.banking.models.Account;

import java.time.LocalDateTime;
import com.bootcamp.banking.models.Transaction;

@Service
public class AccountService {

    private final List<Account> accounts = new ArrayList<>();
    private final AtomicLong nextAccountId = new AtomicLong(1);
    private final List<Transaction> transactions = new ArrayList<>();
    private final AtomicLong nextTransactionId = new AtomicLong(1);

    public Account createAccount(Account accountRequest) {
        Account account = new Account(
                String.valueOf(nextAccountId.getAndIncrement()),
                accountRequest.getUserId(),
                accountRequest.getAccountType(),
                BigDecimal.ZERO
        );

        accounts.add(account);
        return account;
    }

    public Account getAccountById(String accountId) {
        for (Account account : accounts) {
            if (account.getAccountId().equals(accountId)) {
                return account;
            }
        }

        return null;
    }

    public Account deposit(String accountId, BigDecimal amount) {
        Account account = getAccountById(accountId);

        if (account == null) {
            return null;
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Deposit amount must be positive");
        }

        account.setBalance(account.getBalance().add(amount));

        Transaction transaction = new Transaction(
                String.valueOf(nextTransactionId.getAndIncrement()),
                accountId,
                "DEPOSIT",
                amount,
                LocalDateTime.now()
        );

        transactions.add(transaction);
        return account;
    }

    public Account withdraw(String accountId, BigDecimal amount) {
        Account account = getAccountById(accountId);

        if (account == null) {
            return null;
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Withdrawal amount must be positive");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(amount));

        Transaction transaction = new Transaction(
                String.valueOf(nextTransactionId.getAndIncrement()),
                accountId,
                "WITHDRAW",
                amount,
                LocalDateTime.now()
        );

        transactions.add(transaction);
        return account;
    }


    public List<Transaction> getTransactions(String accountId) {
        List<Transaction> accountTransactions = new ArrayList<>();

        for (Transaction transaction : transactions) {
            if (transaction.getAccountId().equals(accountId)) {
                accountTransactions.add(transaction);
            }
        }

        return accountTransactions;
    }
}
