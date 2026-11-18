package ru.ukhanov.spvb.dto;

import ru.ukhanov.spvb.model.order.TradeOrder;
import ru.ukhanov.spvb.model.order.enums.OrderStatus;
import ru.ukhanov.spvb.model.order.enums.OrderType;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id, String seccode, OrderType type, BigDecimal price,
        long quantity, long filledQuantity, OrderStatus status,
        Long accountId, Instant createdAt
) {
    public static OrderResponse from(TradeOrder o) {
        return new OrderResponse(o.getId(), o.getSeccode(), o.getType(), o.getPrice(),
                o.getQuantity(), o.getFilledQuantity(), o.getStatus(), o.getAccountId(), o.getCreatedAt());
    }
}
