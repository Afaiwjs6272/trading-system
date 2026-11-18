package ru.ukhanov.spvb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ukhanov.spvb.dto.CreateInstrumentRequest;
import ru.ukhanov.spvb.dto.InstrumentResponse;
import ru.ukhanov.spvb.exception.ApiException;
import ru.ukhanov.spvb.model.instrument.Instrument;
import ru.ukhanov.spvb.repository.InstrumentRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InstrumentService {

    private final InstrumentRepository repo;

    @Transactional
    public InstrumentResponse create(CreateInstrumentRequest req) {
        if (repo.existsById(req.seccode())) {
            throw new ApiException("Instrument already exists: " + req.seccode());
        }
        Instrument i = Instrument.builder()
                .seccode(req.seccode())
                .type(req.type())
                .lotSize(req.lotSize())
                .minStep(req.minStep())
                .build();
        return InstrumentResponse.from(repo.save(i));
    }

    @Transactional(readOnly = true)
    public List<InstrumentResponse> findAll() {
        return repo.findAll().stream()
                .map(InstrumentResponse::from)
                .toList();
    }
}