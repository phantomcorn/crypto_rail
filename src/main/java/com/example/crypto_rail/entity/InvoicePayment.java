package com.example.crypto_rail.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Table(name="invoice_payments")
@Entity
@Builder
@AllArgsConstructor
public class InvoicePayment {

    @Id
    @Column(name="invoice_hash_id", length=66, nullable = false, unique = true)
    String invoiceHashId;

    @Column(name="block_number", length=256, nullable = false)
    String blockNumber;

    @Column(name="block_hash", length = 66, nullable = false)
    String blockHash;
}
