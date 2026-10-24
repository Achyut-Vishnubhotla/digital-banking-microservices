package com.bank.account.service;

import com.bank.account.config.CacheConfig;
import com.bank.account.dto.AccountDto;
import com.bank.account.repo.AccountRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lives in its own bean because Spring's caching works through proxies:
 * a @Cacheable method called from inside the same class would bypass the cache.
 */
@Component
public class AccountLookup {

    private final AccountRepository accounts;

    public AccountLookup(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Cacheable(cacheNames = CacheConfig.ACCOUNT_CACHE, key = "#id")
    @Transactional(readOnly = true)
    public AccountDto byId(Long id) {
        return accounts.findById(id)
                .map(AccountDto::from)
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }
}
