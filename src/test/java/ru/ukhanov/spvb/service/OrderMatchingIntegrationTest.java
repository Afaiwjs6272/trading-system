package ru.ukhanov.spvb.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.ukhanov.spvb.dto.CreateOrderRequest;
import ru.ukhanov.spvb.dto.OrderResponse;
import ru.ukhanov.spvb.exception.ApiException;
import ru.ukhanov.spvb.model.account.Account;
import ru.ukhanov.spvb.model.holding.Holding;
import ru.ukhanov.spvb.model.instrument.Instrument;
import ru.ukhanov.spvb.model.instrument.enums.InstrumentType;
import ru.ukhanov.spvb.model.order.enums.OrderStatus;
import ru.ukhanov.spvb.model.order.enums.OrderType;
import ru.ukhanov.spvb.model.trade.Trade;
import ru.ukhanov.spvb.repository.AccountRepository;
import ru.ukhanov.spvb.repository.HoldingRepository;
import ru.ukhanov.spvb.repository.InstrumentRepository;
import ru.ukhanov.spvb.repository.TradeRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@RequiredArgsConstructor
class OrderMatchingIntegrationTest {

    @Autowired
    OrderService orderService;
    @Autowired
    InstrumentRepository instrumentRepo;
    @Autowired
    AccountRepository accountRepo;
    @Autowired
    HoldingRepository holdingRepo;
    @Autowired
    TradeRepository tradeRepo;

    Long sellerId;
    Long buyerId;

    @BeforeEach
    void setUp() {
        instrumentRepo.save(Instrument.builder()
                .seccode("GAZP").type(InstrumentType.STOCK)
                .lotSize(1).minStep(new BigDecimal("0.01")).build());

        Account seller = accountRepo.save(Account.builder()
                .userId(1L).balance(BigDecimal.ZERO)
                .reservedBalance(BigDecimal.ZERO).currency("RUB").build());
        Account buyer = accountRepo.save(Account.builder()
                .userId(2L).balance(new BigDecimal("100000"))
                .reservedBalance(BigDecimal.ZERO).currency("RUB").build());

        holdingRepo.save(Holding.builder()
                .accountId(seller.getId()).seccode("GAZP")
                .quantity(100).reservedQuantity(0).build());

        sellerId = seller.getId();
        buyerId = buyer.getId();
    }

    @Test
    void fullMatch_producesTrade_andUpdatesBalances() {
        orderService.placeOrder(new CreateOrderRequest(
                "GAZP", OrderType.SELL, new BigDecimal("100.00"), 10, sellerId));
        OrderResponse buy = orderService.placeOrder(new CreateOrderRequest(
                "GAZP", OrderType.BUY, new BigDecimal("105.00"), 10, buyerId));

        assertThat(buy.status()).isEqualTo(OrderStatus.FILLED);

        List<Trade> trades = tradeRepo.findAll();
        assertThat(trades).hasSize(1);
        Trade t = trades.get(0);
        assertThat(t.getPrice()).isEqualByComparingTo("100.00");
        assertThat(t.getQuantity()).isEqualTo(10);

        Account buyer = accountRepo.findById(buyerId).orElseThrow();
        assertThat(buyer.getBalance()).isEqualByComparingTo("99000.00");
        assertThat(buyer.getReservedBalance()).isEqualByComparingTo("0.00");

        Account seller = accountRepo.findById(sellerId).orElseThrow();
        assertThat(seller.getBalance()).isEqualByComparingTo("1000.00");

        Holding sellerHolding = holdingRepo
                .findByAccountIdAndSeccode(sellerId, "GAZP").orElseThrow();
        assertThat(sellerHolding.getQuantity()).isEqualTo(90);
        assertThat(sellerHolding.getReservedQuantity()).isEqualTo(0);

        Holding buyerHolding = holdingRepo
                .findByAccountIdAndSeccode(buyerId, "GAZP").orElseThrow();
        assertThat(buyerHolding.getQuantity()).isEqualTo(10);
    }

    @Test
    void partialMatch_keepsOrderResting_andReleasesReserveCorrectly() {
        OrderResponse sell = orderService.placeOrder(new CreateOrderRequest(
                "GAZP", OrderType.SELL, new BigDecimal("100.00"), 10, sellerId));
        assertThat(sell.status()).isEqualTo(OrderStatus.NEW);

        OrderResponse buy = orderService.placeOrder(new CreateOrderRequest(
                "GAZP", OrderType.BUY, new BigDecimal("100.00"), 4, buyerId));
        assertThat(buy.status()).isEqualTo(OrderStatus.FILLED);

        // Продавец частично исполнен
        OrderResponse sellAfter = orderService.get(sell.id());
        assertThat(sellAfter.status()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(sellAfter.filledQuantity()).isEqualTo(4);

        Account seller = accountRepo.findById(sellerId).orElseThrow();
        assertThat(seller.getBalance()).isEqualByComparingTo("400.00");

        // Отменяем остаток — резерв должен освободиться
        orderService.cancelOrder(sell.id());
        Holding h = holdingRepo.findByAccountIdAndSeccode(sellerId, "GAZP").orElseThrow();
        assertThat(h.getReservedQuantity()).isZero();
        assertThat(h.getQuantity()).isEqualTo(96);
    }

    @Test
    void buyOrder_rejectsInsufficientFunds() {
        assertThatThrownBy(() -> orderService.placeOrder(new CreateOrderRequest(
                "GAZP", OrderType.BUY, new BigDecimal("100.00"), 10000, buyerId)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    void sellOrder_rejectsInsufficientHoldings() {
        assertThatThrownBy(() -> orderService.placeOrder(new CreateOrderRequest(
                "GAZP", OrderType.SELL, new BigDecimal("100.00"), 200, sellerId)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Insufficient holdings");
    }

    @Test
    void priceTimePriority_picksBestPriceFirst() {
        Long seller2 = accountRepo.save(Account.builder()
                .userId(3L).balance(BigDecimal.ZERO)
                .reservedBalance(BigDecimal.ZERO).currency("RUB").build()).getId();
        holdingRepo.save(Holding.builder()
                .accountId(seller2).seccode("GAZP").quantity(100).reservedQuantity(0).build());

        OrderResponse sellHigh = orderService.placeOrder(new CreateOrderRequest(
                "GAZP", OrderType.SELL, new BigDecimal("102.00"), 10, seller2));
        OrderResponse sellLow = orderService.placeOrder(new CreateOrderRequest(
                "GAZP", OrderType.SELL, new BigDecimal("101.00"), 10, sellerId));

        // BUY 15 @ 105 → сначала матчится с 101 (лучшая цена), потом с 102
        OrderResponse buy = orderService.placeOrder(new CreateOrderRequest(
                "GAZP", OrderType.BUY, new BigDecimal("105.00"), 15, buyerId));

        assertThat(buy.status()).isEqualTo(OrderStatus.FILLED);
        List<Trade> trades = tradeRepo.findAll();
        assertThat(trades).hasSize(2);
        assertThat(trades.get(0).getPrice()).isEqualByComparingTo("101.00");
        assertThat(trades.get(1).getPrice()).isEqualByComparingTo("102.00");
        assertThat(orderService.get(sellLow.id()).status()).isEqualTo(OrderStatus.FILLED);
        assertThat(orderService.get(sellHigh.id()).status()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
    }
}