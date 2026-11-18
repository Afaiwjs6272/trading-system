package ru.ukhanov.spvb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record DepositHoldingRequest(
        @NotBlank
        String seccode,
        @Positive
        long quantity
) {}