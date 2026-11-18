package ru.ukhanov.spvb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ukhanov.spvb.dto.OrderBookResponse;
import ru.ukhanov.spvb.dto.QuoteResponse;
import ru.ukhanov.spvb.exception.ApiException;
import ru.ukhanov.spvb.model.order.TradeOrder;
import ru.ukhanov.spvb.model.order.enums.OrderStatus;
import ru.ukhanov.spvb.model.order.enums.OrderType;
import ru.ukhanov.spvb.repository.InstrumentRepository;
import ru.ukhanov.spvb.repository.OrderRepository;
import ru.ukhanov.spvb.repository.TradeRepository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class QuoteService {

    private static final List<OrderStatus> ACTIVE = List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED);

    private final OrderRepository orderRepository;
    private final InstrumentRepository instrumentRepository;
    private final TradeRepository tradeRepository;

    @Transactional(readOnly = true)
    public QuoteResponse quote(String seccode) {
        ensureInstrument(seccode);

        List<TradeOrder> bids = orderRepository.findActiveOrders(seccode, OrderType.BUY, ACTIVE);
        List<TradeOrder> asks = orderRepository.findActiveOrders(seccode, OrderType.SELL, ACTIVE);

        BigDecimal bestBid = bids.stream().map(TradeOrder::getPrice)
                .max(Comparator.naturalOrder()).orElse(null);
        BigDecimal bestAsk = asks.stream().map(TradeOrder::getPrice)
                .min(Comparator.naturalOrder()).orElse(null);

        long bidVolume = bids.stream()
                .filter(o -> o.getPrice().equals(bestBid))
                .mapToLong(o -> o.getQuantity() - o.getFilledQuantity()).sum();
        long askVolume = asks.stream()
                .filter(o -> o.getPrice().equals(bestAsk))
                .mapToLong(o -> o.getQuantity() - o.getFilledQuantity()).sum();

        BigDecimal lastPrice = tradeRepository.findBySeccodeOrderByExecutedAtDesc(seccode).stream()
                .findFirst().map(t -> t.getPrice()).orElse(null);

        return new QuoteResponse(seccode, bestBid, bestAsk, lastPrice, bidVolume, askVolume);
    }

    @Transactional(readOnly = true)
    public OrderBookResponse orderBook(String seccode, int depth) {
        ensureInstrument(seccode);
        List<TradeOrder> bids = orderRepository.findActiveOrders(seccode, OrderType.BUY, ACTIVE);
        List<TradeOrder> asks = orderRepository.findActiveOrders(seccode, OrderType.SELL, ACTIVE);

        // bids: price desc; asks: price asc
        Map<BigDecimal, Long> bidAgg = new TreeMap<>(Comparator.reverseOrder());
        Map<BigDecimal, Long> askAgg = new TreeMap<>();
        for (TradeOrder o : bids) {
            bidAgg.merge(o.getPrice(), o.getQuantity() - o.getFilledQuantity(), Long::sum);
        }
        for (TradeOrder o : asks) {
            askAgg.merge(o.getPrice(), o.getQuantity() - o.getFilledQuantity(), Long::sum);
        }

        List<OrderBookResponse.Level> bidLevels = bidAgg.entrySet().stream()
                .limit(depth).map(e -> new OrderBookResponse.Level(e.getKey(), e.getValue())).toList();
        List<OrderBookResponse.Level> askLevels = askAgg.entrySet().stream()
                .limit(depth).map(e -> new OrderBookResponse.Level(e.getKey(), e.getValue())).toList();

        return new OrderBookResponse(seccode, bidLevels, askLevels);
    }

    private void ensureInstrument(String seccode) {
        if (!instrumentRepository.existsById(seccode)) {
            throw new ApiException("Unknown instrument: " + seccode);
        }
    }
}
