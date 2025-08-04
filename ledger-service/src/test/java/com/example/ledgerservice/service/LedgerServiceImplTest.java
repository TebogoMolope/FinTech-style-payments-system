package com.example.ledgerservice.service;

import com.example.ledgerservice.domain.Account;
import com.example.ledgerservice.dto.LedgerTransferRequest;
import com.example.ledgerservice.exception.AccountNotFoundException;
import com.example.ledgerservice.exception.InsufficientFundsException;
import com.example.ledgerservice.repository.AccountRepository;
import com.example.ledgerservice.repository.LedgerEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private LedgerServiceImpl ledgerService;

    @Test
    void applyTransfer_shouldSucceed_whenFundsAreSufficient() {
        // Given
        LedgerTransferRequest request = new LedgerTransferRequest("tx-1", 1L, 2L, BigDecimal.TEN);
        Account fromAccount = new Account(1L, BigDecimal.valueOf(100), 0L);
        Account toAccount = new Account(2L, BigDecimal.valueOf(50), 0L);

        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));

        // When
        ledgerService.applyTransfer(request);

        // Then
        verify(accountRepository, times(1)).saveAll(any());
        verify(ledgerEntryRepository, times(1)).saveAll(any());
    }

    @Test
    void applyTransfer_shouldThrowInsufficientFundsException_whenBalanceIsTooLow() {
        // Given
        LedgerTransferRequest request = new LedgerTransferRequest("tx-1", 1L, 2L, BigDecimal.valueOf(200));
        Account fromAccount = new Account(1L, BigDecimal.valueOf(100), 0L);
        Account toAccount = new Account(2L, BigDecimal.valueOf(50), 0L);

        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));

        // When & Then
        assertThrows(InsufficientFundsException.class, () -> ledgerService.applyTransfer(request));
    }

    @Test
    void applyTransfer_shouldThrowAccountNotFoundException_whenAccountDoesNotExist() {
        // Given
        LedgerTransferRequest request = new LedgerTransferRequest("tx-1", 1L, 2L, BigDecimal.TEN);
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AccountNotFoundException.class, () -> ledgerService.applyTransfer(request));
    }

    @Test
    void applyTransfer_shouldBeIdempotent_whenDuplicateTransferId() {
        // Given
        LedgerTransferRequest request = new LedgerTransferRequest("tx-1", 1L, 2L, BigDecimal.TEN);
        Account fromAccount = new Account(1L, BigDecimal.valueOf(100), 0L);
        Account toAccount = new Account(2L, BigDecimal.valueOf(50), 0L);

        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));
        doThrow(new DataIntegrityViolationException("... Unique index or primary key violation: ... IDX_TRANSFER_ID ..."))
                .when(ledgerEntryRepository).saveAll(any());

        // When & Then: The service method should catch the exception and return normally.
        assertDoesNotThrow(() -> ledgerService.applyTransfer(request));
    }
}
