package ru.ukhanov.spvb.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.ukhanov.spvb.dto.CreateInstrumentRequest;
import ru.ukhanov.spvb.dto.InstrumentResponse;
import ru.ukhanov.spvb.service.InstrumentService;

import java.util.List;

@RestController
@RequestMapping("/api/instruments")
@RequiredArgsConstructor
public class InstrumentController {

    private final InstrumentService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstrumentResponse create(@Valid @RequestBody CreateInstrumentRequest req) {
        return service.create(req);
    }

    @GetMapping
    public List<InstrumentResponse> list() { return service.findAll(); }
}
