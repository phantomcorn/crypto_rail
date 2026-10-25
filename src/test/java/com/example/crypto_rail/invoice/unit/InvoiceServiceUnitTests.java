package com.example.crypto_rail.invoice.unit;

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
import static org.mockito.Mockito.times;

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

    @Test
    public void createInvoiceShouldCreateNewInvoiceEntry() {
    }

    @Test
    public void getInvoiceDetailsReturnExistingInvoice() {}

    @Test
    public void getInvoiceReturnsEmptyForNonExistingInvoice() {}

    @Test
    public void detectPaymentOnNonExistingInvoiceWillReturnsFalse() {
        Mockito.when(invoiceRepository.findByHashId(INVALID_HASH_ID)).thenReturn(Optional.empty());
        Assertions.assertFalse(invoiceService.detectPayment(INVALID_HASH_ID));
    }

    @Test
    public void detectPaymentOnExistingInvoiceWillReturnTrue() {
        Mockito.when(invoiceRepository.findByHashId(VALID_HASH_ID)).thenReturn(Optional.of(UNPAID_INVOICE));
        Assertions.assertTrue(invoiceService.detectPayment(VALID_HASH_ID));
    }

    @Test
    public void detectPaymentDoesNotProcessConfirmedInvoiceWillReturnFalse() {
        Mockito.when(invoiceRepository.findByHashId(VALID_HASH_ID)).thenReturn(Optional.of(CONFIRMED_INVOICE));
        Assertions.assertFalse(invoiceService.detectPayment(VALID_HASH_ID));
    }

    @Test
    public void detectPaymentDoesNotProcessDetectedInvoiceWillReturnFalse() {
        Mockito.when(invoiceRepository.findByHashId(VALID_HASH_ID)).thenReturn(Optional.of(DETECTED_INVOICE));
        Assertions.assertFalse(invoiceService.detectPayment(VALID_HASH_ID));
    }

    @Test
    public void detectPaymentDoesNotProcessReorgInvoiceWillReturnFalse() {
        Mockito.when(invoiceRepository.findByHashId(VALID_HASH_ID)).thenReturn(Optional.of(REORG_INVOICE));
        Assertions.assertFalse(invoiceService.detectPayment(VALID_HASH_ID));
    }

    @Test
    public void detectPaymentOnlyProcessUnpaidInvoiceWillReturnTrue() {
        Mockito.when(invoiceRepository.findByHashId(VALID_HASH_ID)).thenReturn(Optional.of(UNPAID_INVOICE));
        Assertions.assertTrue(invoiceService.detectPayment(VALID_HASH_ID));
    }

    @Test
    public void revertOnNonExistingInvoiceWillReturnsFalse() {
        Mockito.when(invoiceRepository.findByHashId(INVALID_HASH_ID)).thenReturn(Optional.empty());
        Assertions.assertFalse(invoiceService.revert(INVALID_HASH_ID));
    }

    @Test
    public void revertOnExistingInvoiceWillReturnTrue() {
        Mockito.when(invoiceRepository.findByHashId(VALID_HASH_ID)).thenReturn(Optional.of(CONFIRMED_INVOICE));
        Assertions.assertTrue(invoiceService.revert(VALID_HASH_ID));
    }

    @Test
    public void revertDoesNotProcessUnpaidInvoiceWillReturnFalse() {
        Mockito.when(invoiceRepository.findByHashId(VALID_HASH_ID)).thenReturn(Optional.of(UNPAID_INVOICE));
        Assertions.assertFalse(invoiceService.revert(VALID_HASH_ID));
    }

    @Test
    public void revertDoesNotProcessDetectedInvoiceWillReturnFalse() {
        Mockito.when(invoiceRepository.findByHashId(VALID_HASH_ID)).thenReturn(Optional.of(DETECTED_INVOICE));
        Assertions.assertFalse(invoiceService.revert(VALID_HASH_ID));
    }

    @Test
    public void revertDoesNotProcessReorgInvoiceWillReturnFalse() {
        Mockito.when(invoiceRepository.findByHashId(VALID_HASH_ID)).thenReturn(Optional.of(UNPAID_INVOICE));
        Assertions.assertFalse(invoiceService.revert(VALID_HASH_ID));
    }

    @Test
    public void revertOnlyProcessConfirmedInvoiceWillReturnTrue() {
        Mockito.when(invoiceRepository.findByHashId(VALID_HASH_ID)).thenReturn(Optional.of(CONFIRMED_INVOICE));
        Assertions.assertTrue(invoiceService.revert(VALID_HASH_ID));
    }

    @Test
    public void detectPaymentAllWithAllUnpaidInvoiceWillReturnTrue() {
        List<String> allValid = List.of(VALID_HASH_ID, VALID_HASH_ID);

        Mockito.when(invoiceRepository.findAllByHashIdIn(allValid)).thenReturn(List.of(UNPAID_INVOICE, UNPAID_INVOICE));
        Assertions.assertTrue(invoiceService.detectPaymentAll(allValid));
    }

    @Test
    public void detectPaymentAllWithNoUnpaidInvoiceWillReturnFalse() {
        List<String> allValid = List.of(VALID_HASH_ID, VALID_HASH_ID, VALID_HASH_ID);

        Mockito.when(invoiceRepository.findAllByHashIdIn(allValid)).thenReturn(List.of(DETECTED_INVOICE, REORG_INVOICE, CONFIRMED_INVOICE));
        Assertions.assertFalse(invoiceService.detectPaymentAll(allValid));
    }

    @Test
    public void detectPaymentAllWillOnlyUpdateUnpaidInvoice() {
        List<String> allValid = List.of(VALID_HASH_ID, VALID_HASH_ID, VALID_HASH_ID, VALID_HASH_ID);

        Mockito.when(invoiceRepository.findAllByHashIdIn(allValid)).thenReturn(List.of(UNPAID_INVOICE, DETECTED_INVOICE, REORG_INVOICE, CONFIRMED_INVOICE));
        Assertions.assertTrue(invoiceService.detectPaymentAll(allValid));
        Mockito.verify(invoiceRepository, times(1)).saveAllAndFlush(List.of(UNPAID_INVOICE));
    }

    @Test
    public void toFinalityWithAllDetectedInvoiceWillReturnTrue() {
        List<String> allValid = List.of(VALID_HASH_ID, VALID_HASH_ID);

        Mockito.when(invoiceRepository.findAllByHashIdIn(allValid)).thenReturn(List.of(DETECTED_INVOICE, DETECTED_INVOICE));
        Assertions.assertTrue(invoiceService.toFinality(allValid));
    }

    @Test
    public void toFinalityWithNoDetectedInvoiceWillReturnFalse() {
        List<String> allValid = List.of(VALID_HASH_ID, VALID_HASH_ID, VALID_HASH_ID);

        Mockito.when(invoiceRepository.findAllByHashIdIn(allValid)).thenReturn(List.of(UNPAID_INVOICE, REORG_INVOICE, CONFIRMED_INVOICE));
        Assertions.assertFalse(invoiceService.toFinality(allValid));
    }

    @Test
    public void toFinalityWillOnlyUpdateOnDetectedInvoice() {
        List<String> allValid = List.of(VALID_HASH_ID, VALID_HASH_ID, VALID_HASH_ID, VALID_HASH_ID);

        Mockito.when(invoiceRepository.findAllByHashIdIn(allValid)).thenReturn(List.of(UNPAID_INVOICE, DETECTED_INVOICE, REORG_INVOICE, CONFIRMED_INVOICE));
        Assertions.assertTrue(invoiceService.toFinality(allValid));
        Mockito.verify(invoiceRepository, times(1)).saveAllAndFlush(List.of(DETECTED_INVOICE));
    }

    @Test
    public void revertAllWithAllConfirmedInvoiceWillReturnTrue() {
        List<String> allValid = List.of(VALID_HASH_ID, VALID_HASH_ID);

        Mockito.when(invoiceRepository.findAllByHashIdIn(allValid)).thenReturn(List.of(CONFIRMED_INVOICE, CONFIRMED_INVOICE));
        Assertions.assertTrue(invoiceService.revertAll(allValid));
    }

    @Test
    public void revertAllWithNoConfirmedInvoiceWillReturnFalse() {
        List<String> allValid = List.of(VALID_HASH_ID, VALID_HASH_ID, VALID_HASH_ID);

        Mockito.when(invoiceRepository.findAllByHashIdIn(allValid)).thenReturn(List.of(UNPAID_INVOICE, DETECTED_INVOICE, REORG_INVOICE));
        Assertions.assertFalse(invoiceService.revertAll(allValid));
    }

    @Test
    public void revertAllWillOnlyUpdateOnConfirmedInvoice() {
        List<String> allValid = List.of(VALID_HASH_ID, VALID_HASH_ID, VALID_HASH_ID, VALID_HASH_ID);

        Mockito.when(invoiceRepository.findAllByHashIdIn(allValid)).thenReturn(List.of(UNPAID_INVOICE, DETECTED_INVOICE, REORG_INVOICE, CONFIRMED_INVOICE));
        Assertions.assertTrue(invoiceService.revertAll(allValid));
        Mockito.verify(invoiceRepository, times(1)).saveAllAndFlush(List.of(CONFIRMED_INVOICE));
    }


}
