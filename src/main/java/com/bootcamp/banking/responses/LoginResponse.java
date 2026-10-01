package com.bootcamp.banking.responses;

import com.bootcamp.banking.models.UserRole;

public record LoginResponse(String token, String tokenType, long expiresIn,
                            String username, UserRole role, String customerId) {}

