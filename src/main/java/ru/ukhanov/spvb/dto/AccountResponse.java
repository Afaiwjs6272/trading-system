package ru.ukhanov.spvb.dto;

import ru.ukhanov.spvb.model.account.Account;

import java.math.BigDecimal;

public record AccountResponse(Long id, Long userId, BigDecimal balance, BigDecimal reservedBalance, String currency) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getUserId(), a.getBalance(), a.getReservedBalance(), a.getCurrency());
    }
}