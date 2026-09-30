package com.bootcamp.banking.repos;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.bootcamp.banking.models.Account;

public interface AccountRepository
        extends MongoRepository<Account, String> {

    List<Account> findByUserId(String userId);

    List<Account> findByBalanceGreaterThanEqual(BigDecimal threshold);

    boolean existsByUserId(String userId);
}
