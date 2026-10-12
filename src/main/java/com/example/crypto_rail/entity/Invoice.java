package com.example.crypto_rail.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;


@Entity
@Table(name = "invoice")
@Getter @Setter
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="invoice_id", length=64)
    String invoiceId;

    @Column(name="merchant_address", nullable = false, length = 42)
    String merchantAddress;

    @Column(name="amount", precision=64, scale=0)
    BigInteger amount;

    @Column(name="decimals")
    Integer decimal;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    Status status = Status.NOT_PAID;
}
