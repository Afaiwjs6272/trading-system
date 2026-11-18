package ru.ukhanov.spvb.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.ukhanov.spvb.dto.AccountResponse;
import ru.ukhanov.spvb.dto.CreateAccountRequest;
import ru.ukhanov.spvb.dto.DepositHoldingRequest;
import ru.ukhanov.spvb.model.holding.Holding;
import ru.ukhanov.spvb.service.AccountService;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id) { return service.get(id); }

    @PostMapping("/{id}/holdings")
    @ResponseStatus(HttpStatus.CREATED)
    public void depositHolding(@PathVariable Long id, @Valid @RequestBody DepositHoldingRequest req) {
        service.depositHolding(id, req);
    }

    @GetMapping("/{id}/holdings")
    public List<Holding> getHoldings(@PathVariable Long id) { return service.getHoldings(id); }
}
