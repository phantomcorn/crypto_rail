package com.example.pyro_cryo.dto.response;

import com.example.pyro_cryo.dto.InvoiceDto;

public record PayResponse(String checkoutUrl, InvoiceDto newInvoice) {}
