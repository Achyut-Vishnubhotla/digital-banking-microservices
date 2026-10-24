package com.bank.account.web;

import com.bank.account.dto.AccountDto;
import com.bank.account.dto.CreateAccountRequest;
import com.bank.account.dto.PageResponse;
import com.bank.account.dto.TransferDto;
import com.bank.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountDto create(Authentication auth, @Valid @RequestBody(required = false) CreateAccountRequest req) {
        return accounts.create(auth.getName(), req == null ? null : req.initialDeposit());
    }

    @GetMapping
    public List<AccountDto> mine(Authentication auth) {
        return accounts.listMine(auth.getName());
    }

    @GetMapping("/{id}")
    public AccountDto get(Authentication auth, @PathVariable Long id) {
        return accounts.getOwned(id, auth.getName());
    }

    @GetMapping("/{id}/transfers")
    public PageResponse<TransferDto> history(Authentication auth, @PathVariable Long id,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return accounts.history(id, auth.getName(), page, size);
    }
}
