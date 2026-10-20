package com.example.crypto_rail.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Table(name="invoice_payments")
@Entity
@Builder
@AllArgsConstructor
@Getter @Setter
public class InvoicePayment {

    @Id
    @Column(name="invoice_hash_id", length=66, nullable = false, unique = true)
    String invoiceHashId;

    @Column(name="block_number", nullable = false, precision = 64)
    BigInteger blockNumber;

    @Column(name="block_hash", length = 66, nullable = false)
    String blockHash;
}
