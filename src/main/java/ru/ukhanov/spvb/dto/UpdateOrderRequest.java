package ru.ukhanov.spvb.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateOrderRequest(
        @NotNull
        @DecimalMin("0.0001")
        BigDecimal price,
        @Positive
        long quantity
) {}