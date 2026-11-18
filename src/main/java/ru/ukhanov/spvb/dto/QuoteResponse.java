package ru.ukhanov.spvb.dto;

import java.math.BigDecimal;

public record QuoteResponse(
        String seccode,
        BigDecimal bestBid,
        BigDecimal bestAsk,
        BigDecimal lastPrice,
        long bidVolume,
        long askVolume
) {}
