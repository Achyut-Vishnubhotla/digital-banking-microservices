package com.bank.account.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transfers", indexes = {
        @Index(name = "idx_transfers_from", columnList = "from_account_id"),
        @Index(name = "idx_transfers_to", columnList = "to_account_id")
})
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Client-supplied key; the UNIQUE constraint makes retries safe (idempotent). */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "from_account_id", nullable = false)
    private Long fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private Long toAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String status = "COMPLETED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Transfer() {
    }

    public Transfer(String idempotencyKey, String createdBy, Long fromAccountId, Long toAccountId, BigDecimal amount) {
        this.idempotencyKey = idempotencyKey;
        this.createdBy = createdBy;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getCreatedBy() { return createdBy; }
    public Long getFromAccountId() { return fromAccountId; }
    public Long getToAccountId() { return toAccountId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
