package ru.ukhanov.spvb.dto;

import jakarta.validation.constraints.*;
import ru.ukhanov.spvb.model.instrument.enums.InstrumentType;

import java.math.BigDecimal;
public record CreateInstrumentRequest(
        @NotBlank
        String seccode,
        @NotNull
        InstrumentType type,
        @Positive
        int lotSize,
        @NotNull
        @DecimalMin("0.0001")
        BigDecimal minStep
) {}