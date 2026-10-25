package com.example.crypto_rail.dto;

import com.example.crypto_rail.entity.Status;

import java.math.BigInteger;

public record InvoiceDto(String id,
                         String hashId,
                         String merchantAddress,
                         String tokenAddress,
                         BigInteger amount,
                         String name,
                         String version,
                         Long chainId,
                         String signature,
                         BigInteger deadline,
                         Status status) {}
