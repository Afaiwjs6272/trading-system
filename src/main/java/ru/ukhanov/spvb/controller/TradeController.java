package ru.ukhanov.spvb.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.ukhanov.spvb.dto.TradeResponse;
import ru.ukhanov.spvb.service.TradeService;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService service;

    @GetMapping("/{id}")
    public TradeResponse get(@PathVariable Long id) { return service.get(id); }

    @GetMapping
    public List<TradeResponse> list(@RequestParam(required = false) String seccode) {
        return seccode == null ? service.findAll() : service.findBySeccode(seccode);
    }
}
