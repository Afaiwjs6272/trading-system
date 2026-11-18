package ru.ukhanov.spvb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ukhanov.spvb.model.trade.Trade;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findBySeccodeOrderByExecutedAtDesc(String seccode);
}
