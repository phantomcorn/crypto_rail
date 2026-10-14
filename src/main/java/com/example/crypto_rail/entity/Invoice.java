package com.example.crypto_rail.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigInteger;


@Entity
@Table(name = "invoice")
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Invoice {

    @Id
    @Column(name="id", length=64)
    String id;

    @Column(name="hash_id", length=66, nullable = false, unique = true)
    String hashId;

    @Column(name="merchant_address", nullable = false, length = 42)
    String merchantAddress;

    @Column(name="token_address", nullable = false, length = 42)
    String tokenAddress;

    @Column(name="amount", precision=64, scale=0)
    BigInteger amount;

    @Column(name="signature", nullable = false)
    String signature;

    @Column(name="signature_expires_at", precision=64, scale=0)
    BigInteger signatureExpiresAt;

    @Column(name="decimals")
    Integer decimal;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    Status status = Status.NOT_PAID;
}
