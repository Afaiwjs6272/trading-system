package ru.ukhanov.spvb.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.ukhanov.spvb.dto.CreateOrderRequest;
import ru.ukhanov.spvb.dto.OrderResponse;
import ru.ukhanov.spvb.dto.UpdateOrderRequest;
import ru.ukhanov.spvb.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@Valid @RequestBody CreateOrderRequest req) {
        return service.placeOrder(req);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) { return service.get(id); }

    @GetMapping(params = "accountId")
    public List<OrderResponse> byAccount(@RequestParam Long accountId) {
        return service.findByAccount(accountId);
    }

    @GetMapping(params = "seccode")
    public List<OrderResponse> bySeccode(@RequestParam String seccode) {
        return service.findBySeccode(seccode);
    }

    @PatchMapping("/{id}")
    public OrderResponse update(@PathVariable Long id, @Valid @RequestBody UpdateOrderRequest req) {
        return service.updateOrder(id, req);
    }

    @DeleteMapping("/{id}")
    public OrderResponse cancel(@PathVariable Long id) { return service.cancelOrder(id); }
}