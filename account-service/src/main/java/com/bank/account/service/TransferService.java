package com.bank.account.service;

import com.bank.account.config.CacheConfig;
import com.bank.account.dto.TransferDto;
import com.bank.account.dto.TransferRequest;
import com.bank.account.model.Account;
import com.bank.account.model.Transfer;
import com.bank.account.repo.AccountRepository;
import com.bank.account.repo.TransferRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class TransferService {

    private final AccountRepository accounts;
    private final TransferRepository transfers;
    private final CacheManager cacheManager;

    public TransferService(AccountRepository accounts, TransferRepository transfers, CacheManager cacheManager) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.cacheManager = cacheManager;
    }

    /**
     * Debit + credit + transfer record happen in ONE database transaction (all or nothing).
     * - Idempotency: the same Idempotency-Key returns the original result instead of moving money twice.
     * - Concurrency: @Version on Account makes a conflicting concurrent update fail (HTTP 409, client retries).
     */
    @Transactional
    public TransferDto transfer(String username, TransferRequest req, String idempotencyKey) {
        Optional<Transfer> existing = transfers.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Transfer t = existing.get();
            if (!t.getCreatedBy().equals(username)) {
                throw new ConflictException("Idempotency key already used");
            }
            return TransferDto.from(t);
        }

        Account from = accounts.findById(req.fromAccountId())
                .filter(a -> a.getOwnerUsername().equals(username))
                .orElseThrow(() -> new NotFoundException("Source account not found"));
        Account to = accounts.findByAccountNumber(req.toAccountNumber())
                .orElseThrow(() -> new NotFoundException("Destination account not found"));

        if (from.getId().equals(to.getId())) {
            throw new BadRequestException("Cannot transfer to the same account");
        }
        BigDecimal amount = req.amount();
        if (from.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient funds");
        }

        from.debit(amount);
        to.credit(amount);
        // Flush now so a version conflict surfaces here (as ObjectOptimisticLockingFailureException), not at commit.
        accounts.saveAllAndFlush(List.of(from, to));
        Transfer saved = transfers.saveAndFlush(
                new Transfer(idempotencyKey, username, from.getId(), to.getId(), amount));

        evictAfterCommit(from.getId(), to.getId());
        return TransferDto.from(saved);
    }

    /** Evict only after commit; evicting earlier lets a concurrent reader re-cache the old balance. */
    private void evictAfterCommit(Long... accountIds) {
        Runnable evict = () -> {
            Cache cache = cacheManager.getCache(CacheConfig.ACCOUNT_CACHE);
            if (cache != null) {
                for (Long id : accountIds) {
                    cache.evict(id);
                }
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict.run();
                }
            });
        } else {
            evict.run();
        }
    }
}
