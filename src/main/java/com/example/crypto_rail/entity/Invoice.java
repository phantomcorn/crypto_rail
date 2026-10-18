package com.example.crypto_rail.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigInteger;


@Entity
@Table(name = "invoices")
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Invoice {

    @Id
    @Column(name="id", length=64)
    String id;

    /* Message */
    @Column(name="hash_id", length=66, nullable = false, unique = true)
    String hashId;

    @Column(name="merchant_address", nullable = false, length = 42)
    String merchantAddress;

    @Column(name="token_address", nullable = false, length = 42)
    String tokenAddress;

    @Column(name="amount", precision=64, scale=0)
    BigInteger amount;

    /* Domain */
    @Column(name="app_name", length=256, nullable = false)
    String name;

    @Column(name="app_version", length=10)
    String version;

    @Min(value = 1, message = "Smallest Chain ID is 1")
    @Column(name="chain_id")
    Long chainId;

    /* Etc. */
    @Column(name="signature", nullable = false)
    String signature;

    @Column(name="signature_expires_at", precision=64, scale=0)
    BigInteger signatureExpiresAt;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    Status status = Status.NOT_PAID;
}
