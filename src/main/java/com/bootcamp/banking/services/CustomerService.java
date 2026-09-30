package com.bootcamp.banking.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bootcamp.banking.models.Customer;
import com.bootcamp.banking.repos.AccountRepository;
import com.bootcamp.banking.repos.CustomerRepository;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    public CustomerService(
            CustomerRepository customerRepository,
            AccountRepository accountRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer getCustomerById(String id) {
        return customerRepository.findById(id).orElse(null);
    }

    public Customer createCustomer(Customer customerRequest) {
        validateName(customerRequest.getName());
        Customer customer = new Customer(null, customerRequest.getName());
        return customerRepository.insert(customer);
    }

    public Customer updateCustomer(String id, Customer customerRequest) {
        Customer customer = getCustomerById(id);

        if (customer == null) {
            return null;
        }

        validateName(customerRequest.getName());
        customer.setName(customerRequest.getName());
        return customerRepository.save(customer);
    }

    public boolean deleteCustomer(String id) {
        if (!customerRepository.existsById(id)) {
            return false;
        }

        if (accountRepository.existsByUserId(id)) {
            throw new IllegalStateException(
                    "Customer still has accounts");
        }

        customerRepository.deleteById(id);
        return true;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }
    }
}
