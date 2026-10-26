package com.example.crypto_rail.invoice.integration;

import com.example.crypto_rail.dto.InvoiceDto;
import com.example.crypto_rail.dto.PayRequestDto;
import com.example.crypto_rail.entity.Invoice;
import com.example.crypto_rail.entity.Status;
import com.example.crypto_rail.repository.InvoiceRepository;
import com.example.crypto_rail.service.InvoiceService;
import com.example.crypto_rail.utils.Web3jClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InvoiceServiceIntegrationTests {

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    InvoiceService invoiceService;

    @Autowired
    Web3jClient web3jClient;

    public void changeStatusTo(String hashId, Status status) {
        Optional<Invoice> found = invoiceRepository.findByHashId(hashId);
        if (found.isEmpty()) {
            return;
        }
        Invoice invoice = found.get();
        invoice.setStatus(status);
    }

    @Test
    public void createInvoiceShouldCreateNewInvoiceEntryWithCorrectTerms() throws IOException {
        PayRequestDto payRequestDto = PayRequestDto.builder()
                .amount(BigInteger.ONE)
                .merchantAddress("00000000000000000000")
                .tokenAddress("22222222222222222222")
                .build();

        Assertions.assertEquals(1, invoiceRepository.findAll().size());

        InvoiceDto newInvoice = invoiceService.createInvoice(payRequestDto).newInvoice();

        Assertions.assertEquals(2, invoiceRepository.findAll().size());

        Assertions.assertEquals(payRequestDto.getAmount(), newInvoice.amount());
        Assertions.assertEquals(payRequestDto.getMerchantAddress(), newInvoice.merchantAddress());
        Assertions.assertEquals(payRequestDto.getTokenAddress(), newInvoice.tokenAddress());
    }

    @Test
    public void detectPaymentShouldUpdateUnpaidInvoiceToDetected() throws IOException {
        PayRequestDto payRequestDto = PayRequestDto.builder()
                .amount(BigInteger.ONE)
                .merchantAddress("00000000000000000000")
                .tokenAddress("22222222222222222222")
                .build();

        InvoiceDto dto = invoiceService.createInvoice(payRequestDto).newInvoice();

        Assertions.assertEquals(Status.NOT_PAID, invoiceRepository.findByHashId(dto.hashId()).get().getStatus());
        Assertions.assertTrue(invoiceService.detectPayment(dto.hashId()));
        Assertions.assertEquals(Status.DETECTED, invoiceRepository.findByHashId(dto.hashId()).get().getStatus());
    }

    @Test
    public void detectPaymentShouldNotUpdateConfirmedInvoiceToDetected() throws IOException {
        PayRequestDto payRequestDto = PayRequestDto.builder()
                .amount(BigInteger.ONE)
                .merchantAddress("00000000000000000000")
                .tokenAddress("22222222222222222222")
                .build();

        InvoiceDto dto = invoiceService.createInvoice(payRequestDto).newInvoice();
        changeStatusTo(dto.hashId(), Status.CONFIRMED);
        Assertions.assertEquals(Status.CONFIRMED, invoiceRepository.findByHashId(dto.hashId()).get().getStatus());
        Assertions.assertFalse(invoiceService.detectPayment(dto.hashId()));
        Assertions.assertEquals(Status.CONFIRMED, invoiceRepository.findByHashId(dto.hashId()).get().getStatus());
    }

    @Test
    public void detectPaymentShouldNotUpdateDetectedInvoiceAgain() throws IOException {
        PayRequestDto payRequestDto = PayRequestDto.builder()
                .amount(BigInteger.ONE)
                .merchantAddress("00000000000000000000")
                .tokenAddress("22222222222222222222")
                .build();

        InvoiceDto dto = invoiceService.createInvoice(payRequestDto).newInvoice();
        changeStatusTo(dto.hashId(), Status.DETECTED);
        Assertions.assertEquals(Status.DETECTED, invoiceRepository.findByHashId(dto.hashId()).get().getStatus());
        Assertions.assertFalse(invoiceService.detectPayment(dto.hashId()));
        Assertions.assertEquals(Status.DETECTED, invoiceRepository.findByHashId(dto.hashId()).get().getStatus());
    }

    @Test
    public void detectPaymentShouldNotUpdateReorgInvoiceToDetected() throws IOException {
        PayRequestDto payRequestDto = PayRequestDto.builder()
                .amount(BigInteger.ONE)
                .merchantAddress("00000000000000000000")
                .tokenAddress("22222222222222222222")
                .build();

        InvoiceDto dto = invoiceService.createInvoice(payRequestDto).newInvoice();
        changeStatusTo(dto.hashId(), Status.REORG);
        Assertions.assertEquals(Status.REORG, invoiceRepository.findByHashId(dto.hashId()).get().getStatus());
        Assertions.assertFalse(invoiceService.detectPayment(dto.hashId()));
        Assertions.assertEquals(Status.REORG, invoiceRepository.findByHashId(dto.hashId()).get().getStatus());
    }

    @Test
    public void revertShouldUpdateDetectedInvoiceToReorg() throws IOException {
        PayRequestDto payRequestDto = PayRequestDto.builder()
                .amount(BigInteger.ONE)
                .merchantAddress("00000000000000000000")
                .tokenAddress("22222222222222222222")
                .build();

        InvoiceDto dto = invoiceService.createInvoice(payRequestDto).newInvoice();
        changeStatusTo(dto.hashId(), Status.DETECTED);
        Assertions.assertEquals(Status.DETECTED, invoiceRepository.findByHashId(dto.hashId()).get().getStatus());
        Assertions.assertTrue(invoiceService.revert(dto.hashId()));
        Assertions.assertEquals(Status.REORG, invoiceRepository.findByHashId(dto.hashId()).get().getStatus());
    }

    @Test
    public void revertShouldNotUpdateUnpaidInvoiceToReorg() throws IOException {
        PayRequestDto payRequestDto = PayRequestDto.builder()
                .amount(BigInteger.ONE)
                .merchantAddress("00000000000000000000")
                .tokenAddress("22222222222222222222")
                .build();

        InvoiceDto dto = invoiceService.createInvoice(payRequestDto).newInvoice();
        changeStatusTo(dto.hashId(), Status.NOT_PAID);
        Assertions.assertEquals(Status.NOT_PAID, invoiceRepository.findByHashId(dto.hashId()).get().getStatus());
        Assertions.assertFalse(invoiceService.revert(dto.hashId()));
        Assertions.assertEquals(Status.NOT_PAID, invoiceRepository.findByHashId(dto.hashId()).get().getStatus());
    }

    @Test
    public void revertShouldNotUpdateReorgInvoiceAgain() throws IOException {
        PayRequestDto payRequestDto = PayRequestDto.builder()
                .amount(BigInteger.ONE)
                .merchantAddress("00000000000000000000")
                .tokenAddress("22222222222222222222")
                .build();

        InvoiceDto dto = invoiceService.createInvoice(payRequestDto).newInvoice();
        changeStatusTo(dto.hashId(), Status.REORG);
        Assertions.assertEquals(Status.REORG, invoiceRepository.findByHashId(dto.hashId()).get().getStatus());
        Assertions.assertFalse(invoiceService.revert(dto.hashId()));
        Assertions.assertEquals(Status.REORG, invoiceRepository.findByHashId(dto.hashId()).get().getStatus());
    }

    @Test
    public void revertShouldNotUpdateConfirmedInvoiceToReorg() throws IOException {
        PayRequestDto payRequestDto = PayRequestDto.builder()
                .amount(BigInteger.ONE)
                .merchantAddress("00000000000000000000")
                .tokenAddress("22222222222222222222")
                .build();

        InvoiceDto dto = invoiceService.createInvoice(payRequestDto).newInvoice();
        changeStatusTo(dto.hashId(), Status.CONFIRMED);
        Assertions.assertEquals(Status.CONFIRMED, invoiceRepository.findByHashId(dto.hashId()).get().getStatus());
        Assertions.assertFalse(invoiceService.revert(dto.hashId()));
        Assertions.assertEquals(Status.CONFIRMED, invoiceRepository.findByHashId(dto.hashId()).get().getStatus());
    }

    @Test
    public void revertAllShouldUpdateOnlyDetectedInvoiceToReorg() {

        List<Invoice> invoices = invoiceRepository.findAll();
        List<Invoice> detectedInvoices = invoices.stream().filter(invoice -> invoice.getStatus().equals(Status.DETECTED)).toList();
        List<Invoice> notDetectedInvoices = invoices.stream().filter(invoice -> !invoice.getStatus().equals(Status.DETECTED)).toList();

        invoiceService.revertAll(invoices.stream().map(Invoice::getHashId).toList());

        detectedInvoices.forEach(invoice -> {
            Invoice updatedInvoice = invoiceRepository.findByHashId(invoice.getHashId()).get();
            Assertions.assertEquals(Status.REORG, updatedInvoice.getStatus());
        });

        notDetectedInvoices.forEach(invoice -> {
            Invoice updatedInvoice = invoiceRepository.findByHashId(invoice.getHashId()).get();
            Assertions.assertEquals(invoice.getStatus(), updatedInvoice.getStatus());
        });
    }

    @Test
    public void toFinalityShouldUpdateOnlyDetectedInvoiceToConfirmed() {
        List<Invoice> invoices = invoiceRepository.findAll();
        List<Invoice> detectedInvoices = invoices.stream().filter(invoice -> invoice.getStatus().equals(Status.DETECTED)).toList();
        List<Invoice> notDetectedInvoices = invoices.stream().filter(invoice -> !invoice.getStatus().equals(Status.DETECTED)).toList();

        invoiceService.toFinality(invoices.stream().map(Invoice::getHashId).toList());

        detectedInvoices.forEach(invoice -> {
            Invoice updatedInvoice = invoiceRepository.findByHashId(invoice.getHashId()).get();
            Assertions.assertEquals(Status.CONFIRMED, updatedInvoice.getStatus());
        });

        notDetectedInvoices.forEach(invoice -> {
            Invoice updatedInvoice = invoiceRepository.findByHashId(invoice.getHashId()).get();
            Assertions.assertEquals(invoice.getStatus(), updatedInvoice.getStatus());
        });
    }

    @Test
    public void detectPaymentAllShouldUpdateOnlyUnpaidInvoiceToDetected() {
        List<Invoice> invoices = invoiceRepository.findAll();
        List<Invoice> unpaidInvoices = invoices.stream().filter(invoice -> invoice.getStatus().equals(Status.NOT_PAID)).toList();
        List<Invoice> notUnpaidInvoices = invoices.stream().filter(invoice -> !invoice.getStatus().equals(Status.NOT_PAID)).toList();

        invoiceService.toFinality(invoices.stream().map(Invoice::getHashId).toList());

        unpaidInvoices.forEach(invoice -> {
            Invoice updatedInvoice = invoiceRepository.findByHashId(invoice.getHashId()).get();
            Assertions.assertEquals(Status.CONFIRMED, updatedInvoice.getStatus());
        });

        notUnpaidInvoices.forEach(invoice -> {
            Invoice updatedInvoice = invoiceRepository.findByHashId(invoice.getHashId()).get();
            Assertions.assertEquals(invoice.getStatus(), updatedInvoice.getStatus());
        });
    }
}
