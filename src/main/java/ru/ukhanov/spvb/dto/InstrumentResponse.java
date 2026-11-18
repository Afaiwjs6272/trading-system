package ru.ukhanov.spvb.dto;

import ru.ukhanov.spvb.model.instrument.Instrument;
import ru.ukhanov.spvb.model.instrument.enums.InstrumentType;

import java.math.BigDecimal;

public record InstrumentResponse(String seccode, InstrumentType type, int lotSize, BigDecimal minStep) {
    public static InstrumentResponse from(Instrument i) {
        return new InstrumentResponse(i.getSeccode(), i.getType(), i.getLotSize(), i.getMinStep());
    }
}