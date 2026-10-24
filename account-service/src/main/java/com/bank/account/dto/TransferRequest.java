package com.bank.account.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record TransferRequest(
        @NotNull Long fromAccountId,
        @NotBlank @Pattern(regexp = "^\\d{12}$", message = "must be 12 digits") String toAccountNumber,
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount) {
}
