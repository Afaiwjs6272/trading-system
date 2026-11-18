package ru.ukhanov.spvb.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.ukhanov.spvb.dto.OrderBookResponse;
import ru.ukhanov.spvb.dto.QuoteResponse;
import ru.ukhanov.spvb.service.QuoteService;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService service;

    @GetMapping("/{seccode}")
    public QuoteResponse quote(@PathVariable String seccode) {
        return service.quote(seccode);
    }

    @GetMapping("/{seccode}/orderbook")
    public OrderBookResponse orderBook(@PathVariable String seccode,
                                       @RequestParam(defaultValue = "10") int depth) {
        return service.orderBook(seccode, depth);
    }
}