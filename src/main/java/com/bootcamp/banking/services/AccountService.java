package com.bootcamp.banking.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bootcamp.banking.models.Account;
import com.bootcamp.banking.models.Transaction;
import com.bootcamp.banking.repos.AccountRepository;
import com.bootcamp.banking.repos.CustomerRepository;
import com.bootcamp.banking.repos.TransactionRepository;
import com.bootcamp.banking.responses.TransferResponse;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;

    private static final BigDecimal MAX_TRANSFER_AMOUNT =
            new BigDecimal("10000.00");

    public AccountService(
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            TransactionRepository transactionRepository,
            AuditService auditService) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
    }

    public Account createAccount(Account accountRequest) {
        String userId = accountRequest.getUserId();
        String accountType = accountRequest.getAccountType();

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID is required");
        }

        if (accountType == null || accountType.isBlank()) {
            throw new IllegalArgumentException("Account type is required");
        }

        if (!customerRepository.existsById(userId)) {
            throw new NoSuchElementException("Customer not found");
        }

        Account account = new Account(
                null,
                userId,
                accountType,
                BigDecimal.ZERO
        );

        return accountRepository.insert(account);
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public List<Account> getAccountsByUserId(String userId) {
        return accountRepository.findByUserId(userId);
    }

    public List<Account> getPremiumAccounts(BigDecimal threshold) {
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Premium threshold cannot be negative");
        }
        return accountRepository.findByBalanceGreaterThanEqual(threshold);
    }

    public Account getAccountById(String accountId) {
        return accountRepository.findById(accountId).orElse(null);
    }

    public Account updateAccount(String accountId, Account accountRequest) {
        Account account = getAccountById(accountId);

        if (account == null) {
            return null;
        }

        String accountType = accountRequest.getAccountType();
        if (accountType == null || accountType.isBlank()) {
            throw new IllegalArgumentException("Account type is required");
        }

        account.setAccountType(accountType);
        return accountRepository.save(account);
    }

    public boolean deleteAccount(String accountId) {
        if (!accountRepository.existsById(accountId)) {
            return false;
        }

        accountRepository.deleteById(accountId);
        return true;
    }

    @Transactional
    public Account deposit(String accountId, BigDecimal amount) {
        Account account = getAccountById(accountId);

        if (account == null) {
            auditService.recordFailure("DEPOSIT", null, accountId,
                    null, amount, "Account not found");
            return null;
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            auditService.recordFailure("DEPOSIT", account.getUserId(),
                    accountId, null, amount,
                    "Deposit amount must be positive");
            throw new IllegalArgumentException(
                    "Deposit amount must be positive");
        }

        account.setBalance(account.getBalance().add(amount));
        Account updatedAccount = accountRepository.save(account);

        Transaction transaction = new Transaction(
                null,
                accountId,
                "DEPOSIT",
                amount,
                LocalDateTime.now()
        );

        transactionRepository.insert(transaction);
        auditService.recordSuccess("DEPOSIT", account.getUserId(),
                accountId, null, amount);
        return updatedAccount;
    }

    @Transactional
    public Account withdraw(String accountId, BigDecimal amount) {
        Account account = getAccountById(accountId);

        if (account == null) {
            auditService.recordFailure("WITHDRAW", null, accountId,
                    null, amount, "Account not found");
            return null;
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            auditService.recordFailure("WITHDRAW", account.getUserId(),
                    accountId, null, amount,
                    "Withdrawal amount must be positive");
            throw new IllegalArgumentException(
                    "Withdrawal amount must be positive");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            auditService.recordFailure("WITHDRAW", account.getUserId(),
                    accountId, null, amount, "Insufficient balance");
            throw new IllegalArgumentException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(amount));
        Account updatedAccount = accountRepository.save(account);

        Transaction transaction = new Transaction(
                null,
                accountId,
                "WITHDRAW",
                amount,
                LocalDateTime.now()
        );

        transactionRepository.insert(transaction);
        auditService.recordSuccess("WITHDRAW", account.getUserId(),
                accountId, null, amount);
        return updatedAccount;
    }

    @Transactional
    public TransferResponse transfer(String fromAccountId,
            String toAccountId, BigDecimal amount) {
        validateTransferInput(fromAccountId, toAccountId, amount);

        Account fromAccount = getAccountById(fromAccountId);
        Account toAccount = getAccountById(toAccountId);

        if (fromAccount == null || toAccount == null) {
            String customerId = fromAccount == null
                    ? null : fromAccount.getUserId();
            auditService.recordFailure("TRANSFER", customerId,
                    fromAccountId, toAccountId, amount,
                    "One or both accounts were not found");
            throw new NoSuchElementException(
                    "One or both accounts were not found");
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            rejectTransfer(fromAccount.getUserId(), fromAccountId,
                    toAccountId, amount, "Insufficient balance");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.saveAll(List.of(fromAccount, toAccount));

        LocalDateTime timestamp = LocalDateTime.now();
        transactionRepository.insert(new Transaction(null, fromAccountId,
                "TRANSFER_OUT", amount, timestamp));
        transactionRepository.insert(new Transaction(null, toAccountId,
                "TRANSFER_IN", amount, timestamp));
        auditService.recordSuccess("TRANSFER", fromAccount.getUserId(),
                fromAccountId, toAccountId, amount);

        return new TransferResponse(fromAccount, toAccount);
    }

    private void validateTransferInput(String fromAccountId,
            String toAccountId, BigDecimal amount) {
        if (fromAccountId == null || fromAccountId.isBlank()
                || toAccountId == null || toAccountId.isBlank()) {
            rejectTransfer(null, fromAccountId, toAccountId, amount,
                    "Both account IDs are required");
        }
        if (fromAccountId.equals(toAccountId)) {
            Account account = getAccountById(fromAccountId);
            rejectTransfer(account == null ? null : account.getUserId(),
                    fromAccountId, toAccountId, amount,
                    "Source and destination accounts must be different");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            rejectTransfer(null, fromAccountId, toAccountId, amount,
                    "Transfer amount must be positive");
        }
        if (amount.compareTo(MAX_TRANSFER_AMOUNT) > 0) {
            rejectTransfer(null, fromAccountId, toAccountId, amount,
                    "Transfer exceeds the 10000.00 limit");
        }
    }

    private void rejectTransfer(String customerId, String fromAccountId,
            String toAccountId, BigDecimal amount, String reason) {
        auditService.recordFailure("TRANSFER", customerId, fromAccountId,
                toAccountId, amount, reason);
        throw new IllegalArgumentException(reason);
    }

    public List<Transaction> getTransactions(String accountId) {
        return transactionRepository.findByAccountId(accountId);
    }
}
