package com.example.crypto_rail.service;

import com.example.crypto_rail.dto.PayRequestDto;
import com.example.crypto_rail.entity.Invoice;
import com.example.crypto_rail.repository.InvoiceRepository;
import com.example.crypto_rail.web3j.Web3jClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class InvoiceService {

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    Web3jClient web3jClient;

    public String createInvoice(PayRequestDto dto) throws IOException {

        Invoice newInvoice = invoiceRepository.save(
                Invoice.builder()
                .amount(dto.getAmount())
                .decimal(dto.getDecimal())
                .merchantAddress(dto.getMerchantAddress())
                .build()
        );

        dto.setId(newInvoice.getInvoiceId());
        dto.setStatus(newInvoice.getStatus());

        //create voucher (signature + data)
        String voucher = web3jClient.signTypedMesssge(dto);;
        return voucher;
    }
}
