package com.bank.account.repo;

import com.bank.account.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByOwnerUsernameOrderByIdAsc(String ownerUsername);
    Optional<Account> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);
    long countByOwnerUsername(String ownerUsername);
}
