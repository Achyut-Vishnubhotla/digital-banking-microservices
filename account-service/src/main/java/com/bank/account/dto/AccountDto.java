package com.bank.account.dto;

import com.bank.account.model.Account;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountDto(Long id, String accountNumber, String ownerUsername, BigDecimal balance, Instant createdAt) {
    public static AccountDto from(Account a) {
        return new AccountDto(a.getId(), a.getAccountNumber(), a.getOwnerUsername(), a.getBalance(), a.getCreatedAt());
    }
}
