package com.example.crypto_rail.repository;

import com.example.crypto_rail.entity.InvoicePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, String> {
}
