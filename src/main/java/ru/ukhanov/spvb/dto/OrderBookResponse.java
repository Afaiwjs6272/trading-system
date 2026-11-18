package ru.ukhanov.spvb.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderBookResponse(String seccode, List<Level> bids, List<Level> asks) {
    public record Level(BigDecimal price, long quantity) {}
}
