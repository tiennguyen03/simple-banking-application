package com.bootcamp.banking.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Login identity, stored separately from a customer's banking information. */
@Document(collection = "user_credentials")
public class UserCredential {
    @Id
    private String id;
    private String username;
    @JsonIgnore
    private String passwordHash;
    private UserRole role;
    private String customerId;

    public UserCredential() {
    }

    public UserCredential(String username, String passwordHash, UserRole role, String customerId) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.customerId = customerId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
}
