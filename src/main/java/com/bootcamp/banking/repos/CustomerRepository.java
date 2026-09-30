package com.bootcamp.banking.repos;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.bootcamp.banking.models.Customer;

public interface CustomerRepository
        extends MongoRepository<Customer, String> {
}