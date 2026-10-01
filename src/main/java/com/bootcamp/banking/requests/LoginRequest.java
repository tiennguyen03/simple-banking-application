package com.bootcamp.banking.requests;

import jakarta.validation.constraints.*;

public record LoginRequest(@NotBlank @Size(max = 80) String username,
                           @NotBlank @Size(max = 72) String password) {}

