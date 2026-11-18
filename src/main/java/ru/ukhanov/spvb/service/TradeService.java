package ru.ukhanov.spvb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ukhanov.spvb.dto.TradeResponse;
import ru.ukhanov.spvb.exception.ApiException;
import ru.ukhanov.spvb.repository.TradeRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository repo;

    @Transactional(readOnly = true)
    public TradeResponse get(Long id) {
        return repo.findById(id).map(TradeResponse::from)
                .orElseThrow(() -> new ApiException("Unknown trade: " + id));
    }

    @Transactional(readOnly = true)
    public List<TradeResponse> findBySeccode(String seccode) {
        return repo.findBySeccodeOrderByExecutedAtDesc(seccode).stream()
                .map(TradeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TradeResponse> findAll() {
        return repo.findAll().stream().map(TradeResponse::from).toList();
    }
}