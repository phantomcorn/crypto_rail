package com.example.crypto_rail.repository;

import com.example.crypto_rail.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    List<Invoice> findByMerchantAddress(String merchantAddress);
    Optional<Invoice> findByHashId(String hashId);
}

