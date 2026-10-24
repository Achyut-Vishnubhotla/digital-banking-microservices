package com.bank.account;

import com.bank.account.dto.TransferDto;
import com.bank.account.dto.TransferRequest;
import com.bank.account.model.Account;
import com.bank.account.model.Transfer;
import com.bank.account.repo.AccountRepository;
import com.bank.account.repo.TransferRepository;
import com.bank.account.service.BadRequestException;
import com.bank.account.service.NotFoundException;
import com.bank.account.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock AccountRepository accounts;
    @Mock TransferRepository transfers;
    @Mock CacheManager cacheManager;

    TransferService service;
    Account alice;
    Account bob;

    @BeforeEach
    void setUp() {
        service = new TransferService(accounts, transfers, cacheManager);
        alice = account(1L, "alice", "111111111111", "100.00");
        bob = account(2L, "bob", "222222222222", "50.00");
    }

    private static Account account(Long id, String owner, String number, String balance) {
        Account a = new Account(owner, number, new BigDecimal(balance));
        ReflectionTestUtils.setField(a, "id", id);
        return a;
    }

    @Test
    void movesMoneyAndRecordsTransfer() {
        when(transfers.findByIdempotencyKey("k1")).thenReturn(Optional.empty());
        when(accounts.findById(1L)).thenReturn(Optional.of(alice));
        when(accounts.findByAccountNumber("222222222222")).thenReturn(Optional.of(bob));
        when(transfers.saveAndFlush(any(Transfer.class))).thenAnswer(i -> i.getArgument(0));

        TransferDto dto = service.transfer("alice", new TransferRequest(1L, "222222222222", new BigDecimal("30.00")), "k1");

        assertEquals(new BigDecimal("70.00"), alice.getBalance());
        assertEquals(new BigDecimal("80.00"), bob.getBalance());
        assertEquals(new BigDecimal("30.00"), dto.amount());
        assertEquals("COMPLETED", dto.status());
    }

    @Test
    void rejectsInsufficientFunds() {
        when(transfers.findByIdempotencyKey("k2")).thenReturn(Optional.empty());
        when(accounts.findById(1L)).thenReturn(Optional.of(alice));
        when(accounts.findByAccountNumber("222222222222")).thenReturn(Optional.of(bob));

        assertThrows(BadRequestException.class,
                () -> service.transfer("alice", new TransferRequest(1L, "222222222222", new BigDecimal("500.00")), "k2"));

        assertEquals(new BigDecimal("100.00"), alice.getBalance());
        verify(transfers, never()).saveAndFlush(any());
    }

    @Test
    void sameKeyReturnsOriginalResultWithoutMovingMoneyAgain() {
        Transfer original = new Transfer("k3", "alice", 1L, 2L, new BigDecimal("30.00"));
        when(transfers.findByIdempotencyKey("k3")).thenReturn(Optional.of(original));

        TransferDto dto = service.transfer("alice", new TransferRequest(1L, "222222222222", new BigDecimal("30.00")), "k3");

        assertEquals(new BigDecimal("30.00"), dto.amount());
        verifyNoInteractions(accounts);
    }

    @Test
    void cannotSendFromSomeoneElsesAccount() {
        when(transfers.findByIdempotencyKey("k4")).thenReturn(Optional.empty());
        when(accounts.findById(2L)).thenReturn(Optional.of(bob));

        assertThrows(NotFoundException.class,
                () -> service.transfer("alice", new TransferRequest(2L, "111111111111", new BigDecimal("10.00")), "k4"));
        assertEquals(new BigDecimal("50.00"), bob.getBalance());
    }

    @Test
    void cannotTransferToSameAccount() {
        when(transfers.findByIdempotencyKey("k5")).thenReturn(Optional.empty());
        when(accounts.findById(1L)).thenReturn(Optional.of(alice));
        when(accounts.findByAccountNumber("111111111111")).thenReturn(Optional.of(alice));

        assertThrows(BadRequestException.class,
                () -> service.transfer("alice", new TransferRequest(1L, "111111111111", new BigDecimal("10.00")), "k5"));
    }
}
