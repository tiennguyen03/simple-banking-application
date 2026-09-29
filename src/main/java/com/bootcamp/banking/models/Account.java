package com.bootcamp.banking.models;

import java.math.BigDecimal;

public class Account {

    private String accountId;
    private String userId;
    private String accountType;
    private BigDecimal balance;

    public Account() {
    }

    public Account(
            String accountId,
            String userId,
            String accountType,
            BigDecimal balance) {
        this.accountId = accountId;
        this.userId = userId;
        this.accountType = accountType;
        this.balance = balance;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}