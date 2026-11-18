package ru.ukhanov.spvb.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotNull
        Long userId,
        @NotNull
        @DecimalMin("0.00")
        BigDecimal balance,
        @NotBlank
        @Size(min = 3, max = 3)
        String currency
) {}
