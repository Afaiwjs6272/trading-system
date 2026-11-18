package ru.ukhanov.spvb.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.ukhanov.spvb.model.order.enums.OrderType;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotBlank
        String seccode,
        @NotNull
        OrderType type,
        @NotNull
        @DecimalMin("0.0001")
        BigDecimal price,
        @Positive
        long quantity,
        @NotNull
        Long accountId
) {}