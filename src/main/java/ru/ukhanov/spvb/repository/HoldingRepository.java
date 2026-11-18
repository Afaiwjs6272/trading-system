package ru.ukhanov.spvb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ukhanov.spvb.model.holding.Holding;

import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    Optional<Holding> findByAccountIdAndSeccode(Long accountId, String seccode);
}