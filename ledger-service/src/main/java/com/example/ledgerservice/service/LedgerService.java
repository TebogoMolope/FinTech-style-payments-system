package com.example.ledgerservice.service;

import com.example.ledgerservice.dto.AccountView;
import com.example.ledgerservice.dto.CreateAccountRequest;
import com.example.ledgerservice.dto.LedgerTransferRequest;
import com.example.ledgerservice.domain.Account;

public interface LedgerService {
    AccountView createAccount(CreateAccountRequest request);
    AccountView getAccount(Long id);
    void applyTransfer(LedgerTransferRequest request);
}
