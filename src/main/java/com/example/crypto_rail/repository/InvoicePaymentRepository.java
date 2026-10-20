package com.example.crypto_rail.repository;

import com.example.crypto_rail.entity.InvoicePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;

@Repository
public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, String> {

    @NativeQuery("SELECT i.* FROM invoice_payments i WHERE ?1 - i.block_number >= ?2")
    List<InvoicePayment> findFinalizedPayment(BigInteger blockNumber, BigInteger blockToFinality);
}
