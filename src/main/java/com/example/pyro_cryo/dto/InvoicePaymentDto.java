package com.example.pyro_cryo.dto;

import java.math.BigInteger;

public record InvoicePaymentDto(String invoiceHashId, BigInteger blockNumber, String blockHash) {}
