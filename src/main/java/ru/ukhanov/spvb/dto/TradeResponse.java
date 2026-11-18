package ru.ukhanov.spvb.dto;

import ru.ukhanov.spvb.model.trade.Trade;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeResponse(Long id, String seccode, BigDecimal price, long quantity,
                            Long buyOrderId, Long sellOrderId, Instant executedAt) {
    public static TradeResponse from(Trade t) {
        return new TradeResponse(t.getId(), t.getSeccode(), t.getPrice(), t.getQuantity(),
                t.getBuyOrderId(), t.getSellOrderId(), t.getExecutedAt());
    }
}