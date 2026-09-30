package com.bootcamp.banking.repos;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.bootcamp.banking.models.Transaction;

public interface TransactionRepository
        extends MongoRepository<Transaction, String> {

    List<Transaction> findByAccountId(String accountId);
}
