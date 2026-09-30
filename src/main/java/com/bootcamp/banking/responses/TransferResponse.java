package com.bootcamp.banking.responses;

import com.bootcamp.banking.models.Account;

public class TransferResponse {

    private Account fromAccount;
    private Account toAccount;

    public TransferResponse(Account fromAccount, Account toAccount) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
    }

    public Account getFromAccount() {
        return fromAccount;
    }

    public Account getToAccount() {
        return toAccount;
    }
}
