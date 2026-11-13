package com.example.pyro_cryo.dto;

import com.example.pyro_cryo.entity.Status;

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
