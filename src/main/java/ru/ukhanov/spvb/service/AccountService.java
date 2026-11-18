package ru.ukhanov.spvb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ukhanov.spvb.dto.AccountResponse;
import ru.ukhanov.spvb.dto.CreateAccountRequest;
import ru.ukhanov.spvb.dto.DepositHoldingRequest;
import ru.ukhanov.spvb.exception.ApiException;
import ru.ukhanov.spvb.model.account.Account;
import ru.ukhanov.spvb.model.holding.Holding;
import ru.ukhanov.spvb.repository.AccountRepository;
import ru.ukhanov.spvb.repository.HoldingRepository;
import ru.ukhanov.spvb.repository.InstrumentRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepo;
    private final HoldingRepository holdingRepo;
    private final InstrumentRepository instrumentRepo;

    @Transactional
    public AccountResponse create(CreateAccountRequest req) {
        Account a = Account.builder()
                .userId(req.userId())
                .balance(req.balance())
                .reservedBalance(BigDecimal.ZERO)
                .currency(req.currency())
                .build();
        return AccountResponse.from(accountRepo.save(a));
    }

    @Transactional(readOnly = true)
    public AccountResponse get(Long id) {
        return AccountResponse.from(findAccount(id));
    }

    @Transactional
    public void depositHolding(Long accountId, DepositHoldingRequest req) {
        findAccount(accountId);
        if (!instrumentRepo.existsById(req.seccode())) {
            throw new ApiException("Unknown instrument: " + req.seccode());
        }
        Holding h = holdingRepo.findByAccountIdAndSeccode(accountId, req.seccode())
                .orElseGet(() -> Holding.builder()
                        .accountId(accountId).seccode(req.seccode()).quantity(0).reservedQuantity(0).build());
        h.setQuantity(h.getQuantity() + req.quantity());
        holdingRepo.save(h);
    }

    @Transactional(readOnly = true)
    public List<Holding> getHoldings(Long accountId) {
        findAccount(accountId);
        return holdingRepo.findAll().stream()
                .filter(h -> h.getAccountId().equals(accountId))
                .toList();
    }

    Account findAccount(Long id) {
        return accountRepo.findById(id)
                .orElseThrow(() -> new ApiException("Unknown account: " + id));
    }
}