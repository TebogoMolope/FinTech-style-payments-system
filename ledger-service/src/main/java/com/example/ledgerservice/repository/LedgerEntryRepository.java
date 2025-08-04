package com.example.ledgerservice.repository;

import com.example.ledgerservice.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    boolean existsByTransferId(String transferId);
}
