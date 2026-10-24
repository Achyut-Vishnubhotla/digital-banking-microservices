package com.bank.account.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_username", nullable = false)
    private String ownerUsername;

    @Column(name = "account_number", nullable = false, unique = true, length = 12)
    private String accountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    /** Optimistic locking: a concurrent update to the same row fails instead of silently overwriting. */
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Account() {
    }

    public Account(String ownerUsername, String accountNumber, BigDecimal balance) {
        this.ownerUsername = ownerUsername;
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public Long getId() { return id; }
    public String getOwnerUsername() { return ownerUsername; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getBalance() { return balance; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
