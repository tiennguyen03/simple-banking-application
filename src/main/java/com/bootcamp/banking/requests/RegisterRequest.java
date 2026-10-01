package com.bootcamp.banking.requests;

import jakarta.validation.constraints.*;

public record RegisterRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{3,80}") String username,
                              @NotBlank @Size(min = 10, max = 72) String password,
                              @NotBlank @Size(max = 120) String name) {}

