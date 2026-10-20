package com.example.crypto_rail.service;

import com.example.crypto_rail.entity.InvoicePayment;
import com.example.crypto_rail.repository.InvoicePaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;

@Service
public class InvoicePaymentService {

    private final Integer BLOCKS_TO_FINALITY = 12;

    @Autowired
    InvoicePaymentRepository invoicePaymentRepository;

    public void process(String invoiceHashId, BigInteger blockNumber, String blockHash) {
        invoicePaymentRepository.save(
                InvoicePayment.builder()
                        .invoiceHashId(invoiceHashId)
                        .blockNumber(blockNumber)
                        .blockHash(blockHash)
                        .build()
        );
    }

    public List<InvoicePayment>canFinalize(BigInteger currentBlockNumber) {
        return invoicePaymentRepository.findFinalizedPayment(currentBlockNumber, BigInteger.valueOf(BLOCKS_TO_FINALITY));
    }
}
