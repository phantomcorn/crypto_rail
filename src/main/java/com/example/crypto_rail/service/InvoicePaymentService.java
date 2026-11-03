package com.example.crypto_rail.service;

import com.example.crypto_rail.entity.InvoicePayment;
import com.example.crypto_rail.repository.InvoicePaymentRepository;
import com.example.crypto_rail.dto.InvoicePaymentDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Service
public class InvoicePaymentService {

    private final Integer BLOCKS_TO_FINALITY = 12;

    @Autowired
    InvoicePaymentRepository invoicePaymentRepository;

    public void add(InvoicePaymentDto dto) {
        invoicePaymentRepository.save(toEntity(dto));
    }

    public List<InvoicePaymentDto>canFinalize(BigInteger currentBlockNumber) {
        return invoicePaymentRepository.findFinalizedPayment(currentBlockNumber, BigInteger.valueOf(BLOCKS_TO_FINALITY)).stream().map(InvoicePayment::toDto).toList();
    }

    public List<InvoicePaymentDto> getPaymentBetween(BigInteger from, BigInteger to) {
        //A reorged happened. Sweeped the new block and found these validHashIds
        return invoicePaymentRepository.findByBlockNumberBetween(from, to).stream().map(InvoicePayment::toDto).toList();
    }

    public List<InvoicePaymentDto> getPaymentFrom(BigInteger from) {
        return invoicePaymentRepository.findByBlockNumberGreaterThanEqual(from).stream().map(InvoicePayment::toDto).toList();
    }

    public void saveAll(List<InvoicePaymentDto> payments) {
        invoicePaymentRepository.saveAll(
                payments.stream().map(this::toEntity).toList()
        );
    }

    public Optional<InvoicePaymentDto> findByInvoiceHashId(String invoiceHashId) {
        Optional<InvoicePayment> found = invoicePaymentRepository.findById(invoiceHashId);
        if (found.isEmpty()){
            return Optional.empty();
        }
        InvoicePayment payment = found.get();
        return Optional.of(new InvoicePaymentDto(payment.getInvoiceHashId(), payment.getBlockNumber(), payment.getBlockHash()));
    }

    public void removeAllById(List<String> droppedHashIds) {
        invoicePaymentRepository.deleteAllById(droppedHashIds);
    }

    private InvoicePayment toEntity(InvoicePaymentDto dto) {
        return new InvoicePayment(dto.invoiceHashId(), dto.blockNumber(), dto.blockHash());
    }
}
