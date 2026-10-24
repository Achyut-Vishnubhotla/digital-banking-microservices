package com.bank.account;

import com.bank.account.model.Transfer;
import com.bank.account.repo.TransferRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** Runs against a real PostgreSQL in Docker, so the UNIQUE constraint is the real thing. Needs Docker running. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TransferRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    TransferRepository transfers;

    @Test
    void duplicateIdempotencyKeyIsRejectedByTheDatabase() {
        transfers.saveAndFlush(new Transfer("key-1", "alice", 1L, 2L, new BigDecimal("10.00")));

        assertThrows(DataIntegrityViolationException.class,
                () -> transfers.saveAndFlush(new Transfer("key-1", "alice", 1L, 2L, new BigDecimal("5.00"))));
    }
}
