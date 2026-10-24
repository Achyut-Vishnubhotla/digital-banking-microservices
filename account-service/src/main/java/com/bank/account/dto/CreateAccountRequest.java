package com.bank.account.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

/** initialDeposit is a demo-only convenience so you can try transfers without a real funding flow. */
public record CreateAccountRequest(
        @DecimalMin("0.00") @DecimalMax("1000000.00") @Digits(integer = 7, fraction = 2) BigDecimal initialDeposit) {
}
