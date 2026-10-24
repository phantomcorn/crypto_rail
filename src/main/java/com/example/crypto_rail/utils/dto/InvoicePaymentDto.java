package com.example.crypto_rail.utils.dto;

import java.math.BigInteger;

public record InvoicePaymentDto(String invoiceHashId, BigInteger blockNumber, String blockHash) {}
