package com.bank.account.web;

import com.bank.account.dto.TransferDto;
import com.bank.account.dto.TransferRequest;
import com.bank.account.service.BadRequestException;
import com.bank.account.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transfers;

    public TransferController(TransferService transfers) {
        this.transfers = transfers;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferDto transfer(Authentication auth,
                                @RequestHeader("Idempotency-Key") String idempotencyKey,
                                @Valid @RequestBody TransferRequest req) {
        if (idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new BadRequestException("Idempotency-Key must be 1-100 characters");
        }
        return transfers.transfer(auth.getName(), req, idempotencyKey);
    }
}
