package com.example.crypto_rail.dto.response;

import com.example.crypto_rail.dto.InvoiceDto;

public record PayResponse(String checkoutUrl, InvoiceDto newInvoice) {}
