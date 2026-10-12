package com.example.crypto_rail.repository;

import com.example.crypto_rail.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.List;


public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    List<Invoice> findByMerchantAddress(String merchantAddress);
}

