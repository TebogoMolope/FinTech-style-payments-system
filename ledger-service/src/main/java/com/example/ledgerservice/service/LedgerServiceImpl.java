package com.example.ledgerservice.service;

import com.example.ledgerservice.domain.Account;
import com.example.ledgerservice.domain.LedgerEntry;
import com.example.ledgerservice.domain.LedgerEntryType;
import com.example.ledgerservice.dto.AccountView;
import com.example.ledgerservice.dto.CreateAccountRequest;
import com.example.ledgerservice.dto.LedgerTransferRequest;
import com.example.ledgerservice.exception.AccountNotFoundException;
import com.example.ledgerservice.exception.InsufficientFundsException;
import com.example.ledgerservice.repository.AccountRepository;
import com.example.ledgerservice.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerServiceImpl implements LedgerService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Override
    @Transactional
    public AccountView createAccount(CreateAccountRequest request) {
        Account account = Account.builder()
                .balance(request.getInitialBalance())
                .build();
        account = accountRepository.save(account);
        log.info("Created account with ID: {}", account.getId());
        return toAccountView(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountView getAccount(Long id) {
        return accountRepository.findById(id)
                .map(this::toAccountView)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + id));
    }

    @Override
    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public void applyTransfer(LedgerTransferRequest request) {
        try {
            Account fromAccount = accountRepository.findByIdForUpdate(request.getFromAccountId())
                    .orElseThrow(() -> new AccountNotFoundException("From account not found: " + request.getFromAccountId()));

            Account toAccount = accountRepository.findByIdForUpdate(request.getToAccountId())
                    .orElseThrow(() -> new AccountNotFoundException("To account not found: " + request.getToAccountId()));

            BigDecimal amount = request.getAmount();

            if (fromAccount.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException("Insufficient funds in account: " + fromAccount.getId());
            }

            fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
            toAccount.setBalance(toAccount.getBalance().add(amount));

            accountRepository.saveAll(List.of(fromAccount, toAccount));

            LedgerEntry debitEntry = LedgerEntry.builder()
                    .transferId(request.getTransferId())
                    .accountId(fromAccount.getId())
                    .amount(amount.negate())
                    .type(LedgerEntryType.DEBIT)
                    .build();

            LedgerEntry creditEntry = LedgerEntry.builder()
                    .transferId(request.getTransferId())
                    .accountId(toAccount.getId())
                    .amount(amount)
                    .type(LedgerEntryType.CREDIT)
                    .build();

            ledgerEntryRepository.saveAll(List.of(debitEntry, creditEntry));

            log.info("Applied transfer {}: {} from account {} to account {}",
                    request.getTransferId(), amount, fromAccount.getId(), toAccount.getId());
        } catch (DataIntegrityViolationException e) {
            // This is a simplified check. A real-world app might inspect the exception more deeply.
            if (e.getMessage() != null && e.getMessage().contains("IDX_TRANSFER_ID")) {
                log.warn("Idempotent retry for transfer ID: {}. This is expected and will be suppressed.", request.getTransferId());
                return; // Suppress the exception to fulfill idempotency.
            }
            throw e;
        }
    }

    private AccountView toAccountView(Account account) {
        return AccountView.builder()
                .id(account.getId())
                .balance(account.getBalance())
                .version(account.getVersion())
                .build();
    }
}
