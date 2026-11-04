package com.example.crypto_rail.Invoice;

import com.example.crypto_rail.entity.Invoice;
import com.example.crypto_rail.entity.Status;
import com.example.crypto_rail.repository.InvoiceRepository;
import com.example.crypto_rail.service.InvoiceService;
import com.example.crypto_rail.utils.Web3jClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.crypto.Hash;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.apache.logging.log4j.util.Strings.repeat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvoiceServiceUnitTests {

    @InjectMocks
    InvoiceService invoiceService;

    @Mock
    InvoiceRepository invoiceRepository;

    String INVALID_HASH_ID = "0x" + repeat("ff", 32);
    String VALID_HASH_ID = "0x" + repeat("ab", 32);
    Invoice UNPAID_INVOICE;
    Invoice DETECTED_INVOICE;
    Invoice REORG_INVOICE;
    Invoice CONFIRMED_INVOICE;


    public InvoiceServiceUnitTests() {
        String uuid = UUID.randomUUID().toString();
        UNPAID_INVOICE = Invoice.builder()
            .id(uuid)
            .hashId(Hash.sha3String(uuid))
            .merchantAddress("0x1111111111111111111111111111111111111111")
            .tokenAddress("0x2222222222222222222222222222222222222222")
            .amount(BigInteger.ONE)
            .name(Web3jClient.APP_NAME)
            .version(Web3jClient.APP_VERSION)
            .chainId(Web3jClient.CHAIN_ID)
            .build();
        DETECTED_INVOICE = Invoice.builder()
                .id(uuid)
                .hashId(Hash.sha3String(uuid))
                .merchantAddress("0x1111111111111111111111111111111111111111")
                .tokenAddress("0x2222222222222222222222222222222222222222")
                .amount(BigInteger.ONE)
                .name(Web3jClient.APP_NAME)
                .version(Web3jClient.APP_VERSION)
                .chainId(Web3jClient.CHAIN_ID)
                .status(Status.DETECTED)
                .build();
        REORG_INVOICE = Invoice.builder()
                .id(uuid)
                .hashId(Hash.sha3String(uuid))
                .merchantAddress("0x1111111111111111111111111111111111111111")
                .tokenAddress("0x2222222222222222222222222222222222222222")
                .amount(BigInteger.ONE)
                .name(Web3jClient.APP_NAME)
                .version(Web3jClient.APP_VERSION)
                .chainId(Web3jClient.CHAIN_ID)
                .status(Status.REORG)
                .build();
        CONFIRMED_INVOICE = Invoice.builder()
                .id(uuid)
                .hashId(Hash.sha3String(uuid))
                .merchantAddress("0x1111111111111111111111111111111111111111")
                .tokenAddress("0x2222222222222222222222222222222222222222")
                .amount(BigInteger.ONE)
                .name(Web3jClient.APP_NAME)
                .version(Web3jClient.APP_VERSION)
                .chainId(Web3jClient.CHAIN_ID)
                .status(Status.CONFIRMED)
                .build();

    }

    public String randomHashId() {
        return Hash.sha3String(UUID.randomUUID().toString());
    }

    @Test
    public void createInvoiceShouldCreateNewInvoiceEntry() {
    }

    @Test
    public void getInvoiceDetailsReturnExistingInvoice() {}

    @Test
    public void getInvoiceReturnsEmptyForNonExistingInvoice() {}

    @Test
    public void detectPaymentOnNonExistingInvoiceWillReturnsFalse() {
        Mockito.when(invoiceRepository.lockByStatus(List.of(INVALID_HASH_ID), Status.NOT_PAID)).thenReturn(List.of());
        Assertions.assertFalse(invoiceService.detectPayment(INVALID_HASH_ID));
    }

    @Test
    public void detectPaymentOnExistingInvoiceWillReturnTrue() {
        Mockito.when(invoiceRepository.lockByStatus(List.of(VALID_HASH_ID), Status.NOT_PAID)).thenReturn(List.of(VALID_HASH_ID));
        Mockito.when(invoiceRepository.transition(List.of(VALID_HASH_ID), Status.NOT_PAID, Status.DETECTED)).thenReturn(1);
        Assertions.assertTrue(invoiceService.detectPayment(VALID_HASH_ID));
    }

    @Test
    public void detectPaymentDoesNotProcessConfirmedInvoiceWillReturnFalse() {
        Assertions.assertFalse(invoiceService.detectPayment(VALID_HASH_ID));
        verify(invoiceRepository, never()).lockByStatus(List.of(VALID_HASH_ID), Status.CONFIRMED);
        verify(invoiceRepository, never()).transition(List.of(VALID_HASH_ID), Status.CONFIRMED, Status.DETECTED);
    }

    @Test
    public void detectPaymentDoesNotProcessDetectedInvoiceWillReturnFalse() {
        Assertions.assertFalse(invoiceService.detectPayment(VALID_HASH_ID));
        verify(invoiceRepository, never()).lockByStatus(List.of(VALID_HASH_ID), Status.DETECTED);
        verify(invoiceRepository, never()).transition(List.of(VALID_HASH_ID), Status.DETECTED, Status.DETECTED);
    }

    @Test
    public void detectPaymentDoesNotProcessReorgInvoiceWillReturnFalse() {
        Assertions.assertFalse(invoiceService.detectPayment(VALID_HASH_ID));
        verify(invoiceRepository, never()).lockByStatus(List.of(VALID_HASH_ID), Status.REORG);
        verify(invoiceRepository, never()).transition(List.of(VALID_HASH_ID), Status.REORG, Status.DETECTED);
    }

    @Test
    public void detectPaymentOnlyProcessUnpaidInvoiceWillReturnTrue() {
        Mockito.when(invoiceRepository.lockByStatus(List.of(VALID_HASH_ID), Status.NOT_PAID)).thenReturn(List.of(VALID_HASH_ID));
        Mockito.when(invoiceRepository.transition(List.of(VALID_HASH_ID), Status.NOT_PAID, Status.DETECTED)).thenReturn(1);
        Assertions.assertTrue(invoiceService.detectPayment(VALID_HASH_ID));
    }

    @Test
    public void revertOnNonExistingInvoiceWillReturnsFalse() {
        Mockito.when(invoiceRepository.lockByStatus(List.of(INVALID_HASH_ID), Status.DETECTED)).thenReturn(List.of());
        Assertions.assertFalse(invoiceService.revert(INVALID_HASH_ID));
    }

    @Test
    public void revertOnExistingInvoiceWillReturnTrue() {
        Mockito.when(invoiceRepository.lockByStatus(List.of(VALID_HASH_ID), Status.DETECTED)).thenReturn(List.of(VALID_HASH_ID));
        Mockito.when(invoiceRepository.transition(List.of(VALID_HASH_ID), Status.DETECTED, Status.REORG)).thenReturn(1);
        Assertions.assertTrue(invoiceService.revert(VALID_HASH_ID));
    }

    @Test
    public void revertDoesNotProcessUnpaidInvoiceWillReturnFalse() {
        Assertions.assertFalse(invoiceService.revert(VALID_HASH_ID));
        verify(invoiceRepository, never()).lockByStatus(List.of(VALID_HASH_ID), Status.NOT_PAID);
        verify(invoiceRepository, never()).transition(List.of(VALID_HASH_ID), Status.NOT_PAID, Status.REORG);
    }

    @Test
    public void revertDoesNotProcessConfirmedInvoiceWillReturnFalse() {
        Assertions.assertFalse(invoiceService.detectPayment(VALID_HASH_ID));
        verify(invoiceRepository, never()).lockByStatus(List.of(VALID_HASH_ID), Status.CONFIRMED);
        verify(invoiceRepository, never()).transition(List.of(VALID_HASH_ID), Status.CONFIRMED, Status.REORG);
    }

    @Test
    public void revertDoesNotProcessReorgInvoiceWillReturnFalse() {
        Assertions.assertFalse(invoiceService.detectPayment(VALID_HASH_ID));
        verify(invoiceRepository, never()).lockByStatus(List.of(VALID_HASH_ID), Status.REORG);
        verify(invoiceRepository, never()).transition(List.of(VALID_HASH_ID), Status.REORG, Status.REORG);
    }

    @Test
    public void revertOnlyProcessDetectedInvoiceWillReturnTrue() {
        Mockito.when(invoiceRepository.lockByStatus(List.of(VALID_HASH_ID), Status.DETECTED)).thenReturn(List.of(VALID_HASH_ID));
        Mockito.when(invoiceRepository.transition(List.of(VALID_HASH_ID), Status.DETECTED, Status.REORG)).thenReturn(1);
        Assertions.assertTrue(invoiceService.revert(VALID_HASH_ID));
    }

    @Test
    public void detectPaymentAllWithAllUnpaidInvoiceWillReturnTrue() {
        List<String> ids = List.of(UNPAID_INVOICE.getHashId(), UNPAID_INVOICE.getHashId(), UNPAID_INVOICE.getHashId(), UNPAID_INVOICE.getHashId());
        Mockito.when(invoiceRepository.lockByStatus(ids, Status.NOT_PAID)).thenReturn(ids);
        Mockito.when(invoiceRepository.transition(ids, Status.NOT_PAID, Status.DETECTED)).thenReturn(ids.size());
        Assertions.assertTrue(invoiceService.detectPaymentAll(ids));
    }

    @Test
    public void detectPaymentAllWithNoUnpaidInvoiceWillReturnFalse() {
        List<String> ids = List.of(REORG_INVOICE.getHashId(), CONFIRMED_INVOICE.getHashId(), DETECTED_INVOICE.getHashId());

        Mockito.when(invoiceRepository.lockByStatus(ids, Status.DETECTED)).thenReturn(List.of());
        Assertions.assertFalse(invoiceService.revertAll(ids));
    }

    @Test
    public void detectPaymentAllWillOnlyUpdateUnpaidInvoice() {
        List<String> ids = List.of(DETECTED_INVOICE.getHashId(), UNPAID_INVOICE.getHashId(), CONFIRMED_INVOICE.getHashId(), UNPAID_INVOICE.getHashId());
        Mockito.when(invoiceRepository.lockByStatus(ids, Status.NOT_PAID)).thenReturn(List.of(UNPAID_INVOICE.getHashId(),UNPAID_INVOICE.getHashId()));
        Mockito.when(invoiceRepository.transition(List.of(UNPAID_INVOICE.getHashId(),UNPAID_INVOICE.getHashId()), Status.NOT_PAID, Status.DETECTED)).thenReturn(2);
        Assertions.assertTrue(invoiceService.detectPaymentAll(ids));
    }

    @Test
    public void toFinalityWithAllDetectedInvoiceWillReturnTrue() {
        List<String> ids = List.of(DETECTED_INVOICE.getHashId(), DETECTED_INVOICE.getHashId(), DETECTED_INVOICE.getHashId(), DETECTED_INVOICE.getHashId());
        Mockito.when(invoiceRepository.lockByStatus(ids, Status.DETECTED)).thenReturn(ids);
        Mockito.when(invoiceRepository.transition(ids, Status.DETECTED, Status.CONFIRMED)).thenReturn(ids.size());
        Assertions.assertTrue(invoiceService.toFinality(ids));
    }

    @Test
    public void toFinalityWithNoDetectedInvoiceWillReturnFalse() {
        List<String> ids = List.of(UNPAID_INVOICE.getHashId(), CONFIRMED_INVOICE.getHashId(), UNPAID_INVOICE.getHashId());

        Mockito.when(invoiceRepository.lockByStatus(ids, Status.DETECTED)).thenReturn(List.of());
        Assertions.assertFalse(invoiceService.revertAll(ids));
    }

    @Test
    public void toFinalityWillOnlyUpdateOnDetectedInvoice() {
        List<String> ids = List.of(DETECTED_INVOICE.getHashId(), UNPAID_INVOICE.getHashId(), CONFIRMED_INVOICE.getHashId(), UNPAID_INVOICE.getHashId());
        Mockito.when(invoiceRepository.lockByStatus(ids, Status.DETECTED)).thenReturn(List.of(DETECTED_INVOICE.getHashId()));
        Mockito.when(invoiceRepository.transition(List.of(DETECTED_INVOICE.getHashId()), Status.DETECTED, Status.CONFIRMED)).thenReturn(1);
        Assertions.assertTrue(invoiceService.toFinality(ids));
    }

    @Test
    public void revertAllWithAllDetectedInvoiceWillReturnTrue() {
        List<String> ids = List.of(DETECTED_INVOICE.getHashId(), DETECTED_INVOICE.getHashId(), DETECTED_INVOICE.getHashId(), DETECTED_INVOICE.getHashId());
        Mockito.when(invoiceRepository.lockByStatus(ids, Status.DETECTED)).thenReturn(ids);
        Mockito.when(invoiceRepository.transition(ids, Status.DETECTED, Status.REORG)).thenReturn(ids.size());
        Assertions.assertTrue(invoiceService.revertAll(ids));
    }

    @Test
    public void revertAllWithNoDetectedInvoiceWillReturnFalse() {
        List<String> ids = List.of(UNPAID_INVOICE.getHashId(), CONFIRMED_INVOICE.getHashId(), UNPAID_INVOICE.getHashId());

        Mockito.when(invoiceRepository.lockByStatus(ids, Status.DETECTED)).thenReturn(List.of());
        Assertions.assertFalse(invoiceService.revertAll(ids));
    }

    @Test
    public void revertAllWillOnlyUpdateOnDetectedInvoice() {
        List<String> ids = List.of(DETECTED_INVOICE.getHashId(), UNPAID_INVOICE.getHashId(), CONFIRMED_INVOICE.getHashId(), UNPAID_INVOICE.getHashId());
        Mockito.when(invoiceRepository.lockByStatus(ids, Status.DETECTED)).thenReturn(List.of(DETECTED_INVOICE.getHashId()));
        Mockito.when(invoiceRepository.transition(List.of(DETECTED_INVOICE.getHashId()), Status.DETECTED, Status.REORG)).thenReturn(1);
        Assertions.assertTrue(invoiceService.revertAll(ids));
    }


}
