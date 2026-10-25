package com.example.crypto_rail.service;

import com.example.crypto_rail.entity.InvoicePayment;
import com.example.crypto_rail.repository.InvoicePaymentRepository;
import com.example.crypto_rail.dto.InvoicePaymentDto;
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

    public List<InvoicePaymentDto>canFinalize(BigInteger currentBlockNumber) {
        return invoicePaymentRepository.findFinalizedPayment(currentBlockNumber, BigInteger.valueOf(BLOCKS_TO_FINALITY)).stream().map(InvoicePayment::toDto).toList();
    }

    public List<InvoicePaymentDto> getPaymentBetween(BigInteger from, BigInteger to) {
        //A reorged happened. Sweeped the new block and found these validHashIds
        return invoicePaymentRepository.findByBlockNumberBetween(from, to).stream().map(InvoicePayment::toDto).toList();
    }

    public void saveAll(List<InvoicePaymentDto> payments) {
        invoicePaymentRepository.saveAll(
                payments.stream().map(this::toEntity).toList()
        );
    }

    public void removeAllById(List<String> droppedHashIds) {
        invoicePaymentRepository.deleteAllById(droppedHashIds);
    }

    private InvoicePayment toEntity(InvoicePaymentDto dto) {
        return new InvoicePayment(dto.invoiceHashId(), dto.blockNumber(), dto.blockHash());
    }
}
