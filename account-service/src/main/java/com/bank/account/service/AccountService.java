package com.bank.account.service;

import com.bank.account.dto.AccountDto;
import com.bank.account.dto.PageResponse;
import com.bank.account.dto.TransferDto;
import com.bank.account.model.Account;
import com.bank.account.model.Transfer;
import com.bank.account.repo.AccountRepository;
import com.bank.account.repo.TransferRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.List;

@Service
public class AccountService {

    private static final int MAX_ACCOUNTS_PER_USER = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accounts;
    private final TransferRepository transfers;
    private final AccountLookup lookup;

    public AccountService(AccountRepository accounts, TransferRepository transfers, AccountLookup lookup) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.lookup = lookup;
    }

    @Transactional
    public AccountDto create(String owner, BigDecimal initialDeposit) {
        if (accounts.countByOwnerUsername(owner) >= MAX_ACCOUNTS_PER_USER) {
            throw new BadRequestException("Account limit reached (" + MAX_ACCOUNTS_PER_USER + ")");
        }
        BigDecimal deposit = (initialDeposit == null ? BigDecimal.ZERO : initialDeposit).setScale(2, RoundingMode.HALF_UP);
        Account account = accounts.save(new Account(owner, newAccountNumber(), deposit));
        return AccountDto.from(account);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> listMine(String owner) {
        return accounts.findByOwnerUsernameOrderByIdAsc(owner).stream().map(AccountDto::from).toList();
    }

    /** Cached read. Returns 404 (not 403) for other people's accounts so IDs can't be probed. */
    public AccountDto getOwned(Long id, String owner) {
        AccountDto dto = lookup.byId(id);
        if (!dto.ownerUsername().equals(owner)) {
            throw new NotFoundException("Account not found");
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public PageResponse<TransferDto> history(Long accountId, String owner, int page, int size) {
        getOwned(accountId, owner);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<Transfer> result = transfers.findByFromAccountIdOrToAccountId(accountId, accountId, pageable);
        return PageResponse.from(result.map(TransferDto::from));
    }

    private String newAccountNumber() {
        String number;
        do {
            number = String.format("%012d", RANDOM.nextLong(1_000_000_000_000L));
        } while (accounts.existsByAccountNumber(number));
        return number;
    }
}
