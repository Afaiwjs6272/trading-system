package ru.ukhanov.spvb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.ukhanov.spvb.model.account.Account;
import ru.ukhanov.spvb.model.holding.Holding;
import ru.ukhanov.spvb.model.order.TradeOrder;
import ru.ukhanov.spvb.model.order.enums.OrderStatus;
import ru.ukhanov.spvb.model.order.enums.OrderType;
import ru.ukhanov.spvb.model.trade.Trade;
import ru.ukhanov.spvb.repository.AccountRepository;
import ru.ukhanov.spvb.repository.HoldingRepository;
import ru.ukhanov.spvb.repository.OrderRepository;
import ru.ukhanov.spvb.repository.TradeRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchingEngine {

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final AccountRepository accountRepository;
    private final HoldingRepository holdingRepository;

    public List<Trade> match(TradeOrder incoming) {
        List<Trade> trades = new ArrayList<>();
        long remaining = incoming.getQuantity() - incoming.getFilledQuantity();

        while (remaining > 0) {
            TradeOrder cp = findBestCounterparty(incoming);
            if (cp == null) break;

            long cpRemaining = cp.getQuantity() - cp.getFilledQuantity();
            if (cpRemaining <= 0) {
                // защита от "зависших" состояний
                break;
            }
            long qty = Math.min(remaining, cpRemaining);
            BigDecimal execPrice = cp.getPrice();

            trades.add(executeTrade(incoming, cp, execPrice, qty));
            remaining -= qty;
        }
        return trades;
    }

    private TradeOrder findBestCounterparty(TradeOrder incoming) {
        List<OrderStatus> active = List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED);
        List<TradeOrder> candidates = incoming.getType() == OrderType.BUY
                ? orderRepository.findMatchableSells(incoming.getSeccode(), OrderType.SELL,
                active, incoming.getPrice(), incoming.getId())
                : orderRepository.findMatchableBuys(incoming.getSeccode(), OrderType.BUY,
                active, incoming.getPrice(), incoming.getId());

        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private Trade executeTrade(TradeOrder incoming, TradeOrder cp,
                               BigDecimal execPrice, long qty) {
        TradeOrder buy = incoming.getType() == OrderType.BUY ? incoming : cp;
        TradeOrder sell = incoming.getType() == OrderType.SELL ? incoming : cp;

        incoming.setFilledQuantity(incoming.getFilledQuantity() + qty);
        cp.setFilledQuantity(cp.getFilledQuantity() + qty);
        updateStatus(incoming);
        updateStatus(cp);
        orderRepository.save(incoming);
        orderRepository.save(cp);

        settleBuy(buy, execPrice, qty);
        settleSell(sell, execPrice, qty);

        Trade trade = Trade.builder()
                .seccode(incoming.getSeccode())
                .price(execPrice)
                .quantity(qty)
                .buyOrderId(buy.getId())
                .sellOrderId(sell.getId())
                .executedAt(Instant.now())
                .build();
        return tradeRepository.save(trade);
    }

    private void settleBuy(TradeOrder buyOrder, BigDecimal execPrice, long qty) {
        Account buyer = accountRepository.findById(buyOrder.getAccountId())
                .orElseThrow();
        // освобождаем резерв по цене заявки
        BigDecimal release = buyOrder.getPrice().multiply(BigDecimal.valueOf(qty));
        buyer.setReservedBalance(buyer.getReservedBalance().subtract(release));
        // фактическая оплата — по цене сделки
        BigDecimal cost = execPrice.multiply(BigDecimal.valueOf(qty));
        buyer.setBalance(buyer.getBalance().subtract(cost));
        accountRepository.save(buyer);

        Holding holding = holdingRepository
                .findByAccountIdAndSeccode(buyOrder.getAccountId(), buyOrder.getSeccode())
                .orElseGet(() -> Holding.builder()
                        .accountId(buyOrder.getAccountId())
                        .seccode(buyOrder.getSeccode())
                        .quantity(0).reservedQuantity(0).build());
        holding.setQuantity(holding.getQuantity() + qty);
        holdingRepository.save(holding);
    }

    private void settleSell(TradeOrder sellOrder, BigDecimal execPrice, long qty) {
        Holding holding = holdingRepository
                .findByAccountIdAndSeccode(sellOrder.getAccountId(), sellOrder.getSeccode())
                .orElseThrow(() -> new IllegalStateException("Holding not found for sell"));
        holding.setReservedQuantity(holding.getReservedQuantity() - qty);
        holding.setQuantity(holding.getQuantity() - qty);
        holdingRepository.save(holding);

        Account seller = accountRepository.findById(sellOrder.getAccountId())
                .orElseThrow();
        BigDecimal proceeds = execPrice.multiply(BigDecimal.valueOf(qty));
        seller.setBalance(seller.getBalance().add(proceeds));
        accountRepository.save(seller);
    }

    private void updateStatus(TradeOrder order) {
        if (order.getFilledQuantity() >= order.getQuantity()) {
            order.setStatus(OrderStatus.FILLED);
        } else if (order.getFilledQuantity() > 0) {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        }
    }
}
