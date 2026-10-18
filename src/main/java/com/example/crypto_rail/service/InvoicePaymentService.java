package com.example.crypto_rail.service;

import com.example.crypto_rail.entity.InvoicePayment;
import com.example.crypto_rail.repository.InvoicePaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InvoicePaymentService {

    @Autowired
    InvoicePaymentRepository invoicePaymentRepository;

    public void process(String invoiceHashId, String blockNumber, String blockHash) {
        invoicePaymentRepository.save(
                InvoicePayment.builder()
                        .invoiceHashId(invoiceHashId)
                        .blockNumber(blockNumber)
                        .blockHash(blockHash)
                        .build()
        );
    }
}
