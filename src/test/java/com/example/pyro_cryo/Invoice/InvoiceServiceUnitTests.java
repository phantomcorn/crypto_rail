package com.example.pyro_cryo.Invoice;

import com.example.pyro_cryo.entity.Invoice;
import com.example.pyro_cryo.entity.Status;
import com.example.pyro_cryo.repository.InvoiceRepository;
import com.example.pyro_cryo.service.InvoiceService;
import com.example.pyro_cryo.utils.Web3jClient;
import com.example.pyro_cryo.utils.webhooks.EventInbox;
import com.example.pyro_cryo.utils.webhooks.EventMessage;
import io.reactivex.processors.PublishProcessor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.web3j.crypto.Hash;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import static org.apache.logging.log4j.util.Strings.repeat;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class InvoiceServiceUnitTests {

    @InjectMocks
    InvoiceService invoiceService;

    @Mock
    InvoiceRepository invoiceRepository;

    @Mock
    EventInbox inbox;

    String INVALID_HASH_ID = "0x" + repeat("ff", 32);
    String VALID_HASH_ID = "0x" + repeat("ab", 32);
    Invoice UNPAID_INVOICE;
    Invoice DETECTED_INVOICE;
    Invoice REORG_INVOICE;
    Invoice CONFIRMED_INVOICE;

    @Mock
    PublishProcessor<EventMessage> publisher;

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
        publisher = mock(PublishProcessor.class);
    }

    @BeforeEach
    public void mockPublisher() {
        when(inbox.getPublisher()).thenReturn(publisher);
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
        verify(publisher, never()).onNext(any(EventMessage.class));
    }

    @Test
    public void detectPaymentOnExistingInvoiceWillReturnTrue() {
        Mockito.when(invoiceRepository.lockByStatus(List.of(VALID_HASH_ID), Status.NOT_PAID)).thenReturn(List.of(UNPAID_INVOICE));
        Mockito.when(invoiceRepository.transition(List.of(UNPAID_INVOICE.getHashId()), Status.NOT_PAID, Status.DETECTED)).thenReturn(1);
        Assertions.assertTrue(invoiceService.detectPayment(VALID_HASH_ID));
        verify(publisher, times(1)).onNext(
                argThat(message ->
                        message.getInvoiceId().equals(UNPAID_INVOICE.getHashId()) &&
                        message.getStatus().equals(Status.DETECTED) &&
                        message.getMerchantAddress().equals(UNPAID_INVOICE.getMerchantAddress())
                )
        );
    }

    @Test
    public void detectPaymentDoesNotProcessConfirmedInvoiceWillReturnFalse() {
        Assertions.assertFalse(invoiceService.detectPayment(VALID_HASH_ID));
        verify(invoiceRepository, never()).lockByStatus(List.of(VALID_HASH_ID), Status.CONFIRMED);
        verify(invoiceRepository, never()).transition(List.of(VALID_HASH_ID), Status.CONFIRMED, Status.DETECTED);
        verify(publisher, never()).onNext(any(EventMessage.class));
    }

    @Test
    public void detectPaymentDoesNotProcessDetectedInvoiceWillReturnFalse() {
        Assertions.assertFalse(invoiceService.detectPayment(VALID_HASH_ID));
        verify(invoiceRepository, never()).lockByStatus(List.of(VALID_HASH_ID), Status.DETECTED);
        verify(invoiceRepository, never()).transition(List.of(VALID_HASH_ID), Status.DETECTED, Status.DETECTED);
        verify(publisher, never()).onNext(any(EventMessage.class));
    }

    @Test
    public void detectPaymentDoesNotProcessReorgInvoiceWillReturnFalse() {
        Assertions.assertFalse(invoiceService.detectPayment(VALID_HASH_ID));
        verify(invoiceRepository, never()).lockByStatus(List.of(VALID_HASH_ID), Status.REORG);
        verify(invoiceRepository, never()).transition(List.of(VALID_HASH_ID), Status.REORG, Status.DETECTED);
        verify(publisher, never()).onNext(any(EventMessage.class));
    }

    @Test
    public void detectPaymentOnlyProcessUnpaidInvoiceWillReturnTrue() {
        Mockito.when(invoiceRepository.lockByStatus(List.of(VALID_HASH_ID), Status.NOT_PAID)).thenReturn(List.of(UNPAID_INVOICE));
        Mockito.when(invoiceRepository.transition(List.of(UNPAID_INVOICE.getHashId()), Status.NOT_PAID, Status.DETECTED)).thenReturn(1);
        Assertions.assertTrue(invoiceService.detectPayment(VALID_HASH_ID));
        verify(publisher, times(1)).onNext(
                argThat(message ->
                        message.getInvoiceId().equals(UNPAID_INVOICE.getHashId()) &&
                                message.getStatus().equals(Status.DETECTED) &&
                                message.getMerchantAddress().equals(UNPAID_INVOICE.getMerchantAddress())
                )
        );
    }

    @Test
    public void revertOnNonExistingInvoiceWillReturnsFalse() {
        Mockito.when(invoiceRepository.lockByStatus(List.of(INVALID_HASH_ID), Status.DETECTED)).thenReturn(List.of());
        Assertions.assertFalse(invoiceService.revert(INVALID_HASH_ID));
        verify(publisher, never()).onNext(any(EventMessage.class));
    }

    @Test
    public void revertOnExistingInvoiceWillReturnTrue() {
        Mockito.when(invoiceRepository.lockByStatus(List.of(VALID_HASH_ID), Status.DETECTED)).thenReturn(List.of(UNPAID_INVOICE));
        Mockito.when(invoiceRepository.transition(List.of(UNPAID_INVOICE.getHashId()), Status.DETECTED, Status.REORG)).thenReturn(1);
        Assertions.assertTrue(invoiceService.revert(VALID_HASH_ID));
        verify(publisher, times(1)).onNext(
                argThat(message ->
                        message.getInvoiceId().equals(UNPAID_INVOICE.getHashId()) &&
                                message.getStatus().equals(Status.REORG) &&
                                message.getMerchantAddress().equals(UNPAID_INVOICE.getMerchantAddress())
                )
        );
    }

    @Test
    public void revertDoesNotProcessUnpaidInvoiceWillReturnFalse() {
        Assertions.assertFalse(invoiceService.revert(VALID_HASH_ID));
        verify(invoiceRepository, never()).lockByStatus(List.of(VALID_HASH_ID), Status.NOT_PAID);
        verify(invoiceRepository, never()).transition(List.of(VALID_HASH_ID), Status.NOT_PAID, Status.REORG);
        verify(publisher, never()).onNext(any(EventMessage.class));
    }

    @Test
    public void revertDoesNotProcessConfirmedInvoiceWillReturnFalse() {
        Assertions.assertFalse(invoiceService.detectPayment(VALID_HASH_ID));
        verify(invoiceRepository, never()).lockByStatus(List.of(VALID_HASH_ID), Status.CONFIRMED);
        verify(invoiceRepository, never()).transition(List.of(VALID_HASH_ID), Status.CONFIRMED, Status.REORG);
        verify(publisher, never()).onNext(any(EventMessage.class));
    }

    @Test
    public void revertDoesNotProcessReorgInvoiceWillReturnFalse() {
        Assertions.assertFalse(invoiceService.detectPayment(VALID_HASH_ID));
        verify(invoiceRepository, never()).lockByStatus(List.of(VALID_HASH_ID), Status.REORG);
        verify(invoiceRepository, never()).transition(List.of(VALID_HASH_ID), Status.REORG, Status.REORG);
        verify(publisher, never()).onNext(any(EventMessage.class));
    }

    @Test
    public void revertOnlyProcessDetectedInvoiceWillReturnTrue() {
        Mockito.when(invoiceRepository.lockByStatus(List.of(VALID_HASH_ID), Status.DETECTED)).thenReturn(List.of(DETECTED_INVOICE));
        Mockito.when(invoiceRepository.transition(List.of(DETECTED_INVOICE.getHashId()), Status.DETECTED, Status.REORG)).thenReturn(1);
        Assertions.assertTrue(invoiceService.revert(VALID_HASH_ID));
        verify(publisher, times(1)).onNext(
                argThat(message ->
                        message.getInvoiceId().equals(DETECTED_INVOICE.getHashId()) &&
                                message.getStatus().equals(Status.REORG) &&
                                message.getMerchantAddress().equals(DETECTED_INVOICE.getMerchantAddress())
                )
        );
    }

    @Test
    public void detectPaymentAllWithAllUnpaidInvoiceWillReturnTrue() {
        List<Invoice> invoices = List.of(UNPAID_INVOICE, UNPAID_INVOICE, UNPAID_INVOICE, UNPAID_INVOICE);
        List<String> ids = invoices.stream().map(Invoice::getHashId).toList();
        Mockito.when(invoiceRepository.lockByStatus(ids, Status.NOT_PAID)).thenReturn(invoices);
        Mockito.when(invoiceRepository.transition(ids, Status.NOT_PAID, Status.DETECTED)).thenReturn(ids.size());
        Assertions.assertTrue(invoiceService.detectPaymentAll(ids));
        verify(publisher, times(ids.size())).onNext(
                argThat(message ->
                        message.getInvoiceId().equals(UNPAID_INVOICE.getHashId()) &&
                                message.getStatus().equals(Status.DETECTED) &&
                                message.getMerchantAddress().equals(UNPAID_INVOICE.getMerchantAddress())
                )
        );
    }

    @Test
    public void detectPaymentAllWithNoUnpaidInvoiceWillReturnFalse() {
        List<String> ids = List.of(REORG_INVOICE.getHashId(), CONFIRMED_INVOICE.getHashId(), DETECTED_INVOICE.getHashId());

        Mockito.when(invoiceRepository.lockByStatus(ids, Status.DETECTED)).thenReturn(List.of());
        Assertions.assertFalse(invoiceService.revertAll(ids));
        verify(publisher, never()).onNext(any(EventMessage.class));
    }

    @Test
    public void detectPaymentAllWillOnlyUpdateUnpaidInvoice() {
        List<String> ids = List.of(DETECTED_INVOICE.getHashId(), UNPAID_INVOICE.getHashId(), CONFIRMED_INVOICE.getHashId(), UNPAID_INVOICE.getHashId());
        Mockito.when(invoiceRepository.lockByStatus(ids, Status.NOT_PAID)).thenReturn(List.of(UNPAID_INVOICE));
        Mockito.when(invoiceRepository.transition(List.of(UNPAID_INVOICE.getHashId()), Status.NOT_PAID, Status.DETECTED)).thenReturn(1);
        Assertions.assertTrue(invoiceService.detectPaymentAll(ids));
        verify(publisher, times(1)).onNext(
                argThat(message ->
                        message.getInvoiceId().equals(UNPAID_INVOICE.getHashId()) &&
                                message.getStatus().equals(Status.DETECTED) &&
                                message.getMerchantAddress().equals(UNPAID_INVOICE.getMerchantAddress())
                )
        );
    }

    @Test
    public void toFinalityWithAllDetectedInvoiceWillReturnTrue() {
        List<Invoice> invoices = List.of(DETECTED_INVOICE, DETECTED_INVOICE, DETECTED_INVOICE, DETECTED_INVOICE);
        List<String> ids = invoices.stream().map(Invoice::getHashId).toList();
        Mockito.when(invoiceRepository.lockByStatus(ids, Status.DETECTED)).thenReturn(invoices);
        Mockito.when(invoiceRepository.transition(ids, Status.DETECTED, Status.CONFIRMED)).thenReturn(ids.size());
        Assertions.assertTrue(invoiceService.toFinality(ids));
        verify(publisher, times(ids.size())).onNext(
                argThat(message ->
                        message.getInvoiceId().equals(UNPAID_INVOICE.getHashId()) &&
                                message.getStatus().equals(Status.CONFIRMED) &&
                                message.getMerchantAddress().equals(UNPAID_INVOICE.getMerchantAddress())
                )
        );
    }

    @Test
    public void toFinalityWithNoDetectedInvoiceWillReturnFalse() {
        List<String> ids = List.of(UNPAID_INVOICE.getHashId(), CONFIRMED_INVOICE.getHashId(), UNPAID_INVOICE.getHashId());

        Mockito.when(invoiceRepository.lockByStatus(ids, Status.DETECTED)).thenReturn(List.of());
        Assertions.assertFalse(invoiceService.revertAll(ids));
        verify(publisher, never()).onNext(any(EventMessage.class));
    }

    @Test
    public void toFinalityWillOnlyUpdateOnDetectedInvoice() {
        List<String> ids = List.of(DETECTED_INVOICE.getHashId(), UNPAID_INVOICE.getHashId(), CONFIRMED_INVOICE.getHashId(), UNPAID_INVOICE.getHashId());
        Mockito.when(invoiceRepository.lockByStatus(ids, Status.DETECTED)).thenReturn(List.of(DETECTED_INVOICE));
        Mockito.when(invoiceRepository.transition(List.of(DETECTED_INVOICE.getHashId()), Status.DETECTED, Status.CONFIRMED)).thenReturn(1);
        Assertions.assertTrue(invoiceService.toFinality(ids));
        verify(publisher, times(1)).onNext(
                argThat(message ->
                        message.getInvoiceId().equals(DETECTED_INVOICE.getHashId()) &&
                                message.getStatus().equals(Status.CONFIRMED) &&
                                message.getMerchantAddress().equals(DETECTED_INVOICE.getMerchantAddress())
                )
        );
    }

    @Test
    public void revertAllWithAllDetectedInvoiceWillReturnTrue() {
        List<Invoice> invoices = List.of(DETECTED_INVOICE, DETECTED_INVOICE, DETECTED_INVOICE, DETECTED_INVOICE);
        List<String> ids = invoices.stream().map(Invoice::getHashId).toList();
        Mockito.when(invoiceRepository.lockByStatus(ids, Status.DETECTED)).thenReturn(invoices);
        Mockito.when(invoiceRepository.transition(ids, Status.DETECTED, Status.REORG)).thenReturn(ids.size());
        Assertions.assertTrue(invoiceService.revertAll(ids));
        verify(publisher, times(ids.size())).onNext(
                argThat(message ->
                        message.getInvoiceId().equals(DETECTED_INVOICE.getHashId()) &&
                                message.getStatus().equals(Status.REORG) &&
                                message.getMerchantAddress().equals(DETECTED_INVOICE.getMerchantAddress())
                )
        );
    }

    @Test
    public void revertAllWithNoDetectedInvoiceWillReturnFalse() {
        List<String> ids = List.of(UNPAID_INVOICE.getHashId(), CONFIRMED_INVOICE.getHashId(), UNPAID_INVOICE.getHashId());

        Mockito.when(invoiceRepository.lockByStatus(ids, Status.DETECTED)).thenReturn(List.of());
        Assertions.assertFalse(invoiceService.revertAll(ids));
        verify(publisher, never()).onNext(any(EventMessage.class));
    }

    @Test
    public void revertAllWillOnlyUpdateOnDetectedInvoice() {
        List<String> ids = List.of(DETECTED_INVOICE.getHashId(), UNPAID_INVOICE.getHashId(), CONFIRMED_INVOICE.getHashId(), UNPAID_INVOICE.getHashId());
        Mockito.when(invoiceRepository.lockByStatus(ids, Status.DETECTED)).thenReturn(List.of(DETECTED_INVOICE));
        Mockito.when(invoiceRepository.transition(List.of(DETECTED_INVOICE.getHashId()), Status.DETECTED, Status.REORG)).thenReturn(1);
        Assertions.assertTrue(invoiceService.revertAll(ids));
        verify(publisher, times(1)).onNext(
                argThat(message ->
                        message.getInvoiceId().equals(DETECTED_INVOICE.getHashId()) &&
                                message.getStatus().equals(Status.REORG) &&
                                message.getMerchantAddress().equals(DETECTED_INVOICE.getMerchantAddress())
                )
        );
    }


}
