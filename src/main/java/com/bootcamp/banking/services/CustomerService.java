package com.bootcamp.banking.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.bootcamp.banking.models.Customer;


@Service
public class CustomerService {
    
    private final List<Customer> customers = new ArrayList<>();

    public CustomerService() {
        customers.add(new Customer("1", "John Doe"));

    }

    public List<Customer> getAllCustomers() {
        return customers;
    }

    public Customer createCustomer(Customer customer) {
        customers.add(customer);
        return customer;
    }

    public Customer getCustomerById(String id) {
        for (Customer customer : customers) {
            if (customer.getId().equals(id)) {
                return customer;
            }
        }

        return null;
    }
}
