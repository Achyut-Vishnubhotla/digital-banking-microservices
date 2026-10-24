package com.bank.account.dto;

import com.bank.account.model.Transfer;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferDto(Long id, Long fromAccountId, Long toAccountId, BigDecimal amount, String status, Instant createdAt) {
    public static TransferDto from(Transfer t) {
        return new TransferDto(t.getId(), t.getFromAccountId(), t.getToAccountId(), t.getAmount(), t.getStatus(), t.getCreatedAt());
    }
}
