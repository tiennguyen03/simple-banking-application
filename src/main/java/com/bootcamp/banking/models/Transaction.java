package com.bootcamp.banking.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {

    private String transactionId;
    private String accountId;
    private String type;
    private BigDecimal amount;
    private LocalDateTime date;

    public Transaction() {
    }

    public Transaction(
            String transactionId,
            String accountId,
            String type,
            BigDecimal amount,
            LocalDateTime date) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.date = date;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}