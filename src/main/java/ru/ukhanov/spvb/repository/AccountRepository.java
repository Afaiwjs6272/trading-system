package ru.ukhanov.spvb.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.ukhanov.spvb.model.account.Account;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserId(Long userId);
}