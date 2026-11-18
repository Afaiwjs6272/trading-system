package ru.ukhanov.spvb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ukhanov.spvb.dto.CreateOrderRequest;
import ru.ukhanov.spvb.dto.OrderResponse;
import ru.ukhanov.spvb.dto.UpdateOrderRequest;
import ru.ukhanov.spvb.exception.ApiException;
import ru.ukhanov.spvb.model.account.Account;
import ru.ukhanov.spvb.model.holding.Holding;
import ru.ukhanov.spvb.model.instrument.Instrument;
import ru.ukhanov.spvb.model.order.TradeOrder;
import ru.ukhanov.spvb.model.order.enums.OrderStatus;
import ru.ukhanov.spvb.model.order.enums.OrderType;
import ru.ukhanov.spvb.repository.AccountRepository;
import ru.ukhanov.spvb.repository.HoldingRepository;
import ru.ukhanov.spvb.repository.InstrumentRepository;
import ru.ukhanov.spvb.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final HoldingRepository holdingRepository;
    private final InstrumentRepository instrumentRepository;
    private final MatchingEngine matchingEngine;
    private final InstrumentLockRegistry lockRegistry;

    @Transactional
    public OrderResponse placeOrder(CreateOrderRequest req) {
        Instrument instrument = instrumentRepository.findById(req.seccode())
                .orElseThrow(() -> new ApiException("Unknown instrument: " + req.seccode()));
        validatePriceStep(req.price(), instrument);
        validateQuantity(req.quantity(), instrument);
        Account account = accountRepository.findById(req.accountId())
                .orElseThrow(() -> new ApiException("Unknown account: " + req.accountId()));

        return lockRegistry.executeWithLock(req.seccode(), () -> {
            TradeOrder order = TradeOrder.builder()
                    .seccode(req.seccode())
                    .type(req.type())
                    .price(req.price())
                    .quantity(req.quantity())
                    .filledQuantity(0)
                    .status(OrderStatus.NEW)
                    .accountId(req.accountId())
                    .createdAt(Instant.now())
                    .build();

            reserve(order, account);
            TradeOrder saved = orderRepository.save(order);
            matchingEngine.match(saved);
            return OrderResponse.from(saved);
        });
    }

    @Transactional
    public OrderResponse updateOrder(Long id, UpdateOrderRequest req) {
        TradeOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ApiException("Unknown order: " + id));
        if (order.getStatus() != OrderStatus.NEW) {
            throw new ApiException("Only NEW orders can be modified (status=" + order.getStatus() + ")");
        }
        Instrument instrument = instrumentRepository.findById(order.getSeccode()).orElseThrow();
        validatePriceStep(req.price(), instrument);
        validateQuantity(req.quantity(), instrument);

        return lockRegistry.executeWithLock(order.getSeccode(), () -> {
            releaseReservation(order, order.getQuantity());
            order.setPrice(req.price());
            order.setQuantity(req.quantity());
            Account account = accountRepository.findById(order.getAccountId()).orElseThrow();
            reserve(order, account);
            orderRepository.save(order);
            matchingEngine.match(order);
            return OrderResponse.from(order);
        });
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        TradeOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ApiException("Unknown order: " + id));
        if (order.getStatus() == OrderStatus.FILLED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new ApiException("Order cannot be cancelled in status " + order.getStatus());
        }
        return lockRegistry.executeWithLock(order.getSeccode(), () -> {
            long remaining = order.getQuantity() - order.getFilledQuantity();
            releaseReservation(order, remaining);
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            return OrderResponse.from(order);
        });
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        return OrderResponse.from(orderRepository.findById(id)
                .orElseThrow(() -> new ApiException("Unknown order: " + id)));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findByAccount(Long accountId) {
        return orderRepository.findByAccountId(accountId).stream()
                .map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findBySeccode(String seccode) {
        return orderRepository.findBySeccodeOrderByCreatedAtDesc(seccode).stream()
                .map(OrderResponse::from).toList();
    }

    private void reserve(TradeOrder order, Account account) {
        if (order.getType() == OrderType.BUY) {
            BigDecimal required = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
            BigDecimal available = account.getBalance().subtract(account.getReservedBalance());
            if (available.compareTo(required) < 0) {
                throw new ApiException("Insufficient funds: required=" + required + ", available=" + available);
            }
            account.setReservedBalance(account.getReservedBalance().add(required));
            accountRepository.save(account);
        } else {
            Holding holding = holdingRepository
                    .findByAccountIdAndSeccode(order.getAccountId(), order.getSeccode())
                    .orElseThrow(() -> new ApiException("No holdings to sell"));
            long available = holding.getQuantity() - holding.getReservedQuantity();
            if (available < order.getQuantity()) {
                throw new ApiException("Insufficient holdings: available=" + available);
            }
            holding.setReservedQuantity(holding.getReservedQuantity() + order.getQuantity());
            holdingRepository.save(holding);
        }
    }

    private void releaseReservation(TradeOrder order, long qty) {
        if (qty <= 0) return;
        if (order.getType() == OrderType.BUY) {
            Account account = accountRepository.findById(order.getAccountId()).orElseThrow();
            BigDecimal release = order.getPrice().multiply(BigDecimal.valueOf(qty));
            account.setReservedBalance(account.getReservedBalance().subtract(release));
            accountRepository.save(account);
        } else {
            Holding holding = holdingRepository
                    .findByAccountIdAndSeccode(order.getAccountId(), order.getSeccode())
                    .orElseThrow();
            holding.setReservedQuantity(holding.getReservedQuantity() - qty);
            holdingRepository.save(holding);
        }
    }

    private void validatePriceStep(BigDecimal price, Instrument instrument) {
        if (price.remainder(instrument.getMinStep()).compareTo(BigDecimal.ZERO) != 0) {
            throw new ApiException("Price must be multiple of minStep " + instrument.getMinStep());
        }
    }

    private void validateQuantity(long quantity, Instrument instrument) {
        if (quantity <= 0 || quantity % instrument.getLotSize() != 0) {
            throw new ApiException("Quantity must be positive multiple of lotSize " + instrument.getLotSize());
        }
    }
}
