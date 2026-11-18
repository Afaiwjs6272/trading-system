package ru.ukhanov.spvb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ukhanov.spvb.model.instrument.Instrument;

public interface InstrumentRepository extends JpaRepository<Instrument, String> {}
