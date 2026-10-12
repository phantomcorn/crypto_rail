package com.example.crypto_rail.service;

import com.example.crypto_rail.dto.PayRequestDto;
import com.example.crypto_rail.entity.Invoice;
import com.example.crypto_rail.repository.InvoiceRepository;
import com.example.crypto_rail.web3j.Web3jClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public class MerchantService {

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    Web3jClient web3jClient;

    public String createInvoice(PayRequestDto dto) throws IOException {
        return"";
    }
}
