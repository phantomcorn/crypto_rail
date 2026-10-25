package com.example.crypto_rail;

import com.example.crypto_rail.entity.Invoice;
import com.example.crypto_rail.entity.Status;
import com.example.crypto_rail.repository.InvoiceRepository;
import com.example.crypto_rail.utils.InvoiceListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.apache.logging.log4j.util.Strings.repeat;

@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class) // Enables Mockito annotations
public class InvoiceListenerTests {

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    InvoiceListener invoiceListener;

    @Test
    public void canRetrieveInitData() {
        List<Invoice> invoices = invoiceRepository.findAll();
        Assertions.assertEquals(1, invoices.size());

        Invoice invoice = invoices.getFirst();
        Assertions.assertEquals("3f2b8c1e-7a4d-4e5b-9c1a-2d6e8f0a1b3c", invoice.getId());
        Assertions.assertEquals("0x" + repeat("ab", 32), invoice.getHashId());
        Assertions.assertEquals("0x1111111111111111111111111111111111111111", invoice.getMerchantAddress());
        Assertions.assertEquals("0x2222222222222222222222222222222222222222", invoice.getTokenAddress());
        Assertions.assertEquals(BigInteger.valueOf(1000000), invoice.getAmount());
        Assertions.assertEquals("CryptoRail", invoice.getName());
        Assertions.assertEquals("0.0.1", invoice.getVersion());
        Assertions.assertEquals(11155111, invoice.getChainId() );
        Assertions.assertEquals("0x" + repeat("cd", 65), invoice.getSignature());
        Assertions.assertEquals(BigInteger.valueOf(1761300000), invoice.getSignatureExpiresAt());
        Assertions.assertEquals("NOT_PAID", invoice.getStatus().toString());
    }

    @Test
    public void shouldListenForUpdate(CapturedOutput output) {

        Optional<Invoice> found = invoiceRepository.findById("3f2b8c1e-7a4d-4e5b-9c1a-2d6e8f0a1b3c");
        Assertions.assertTrue(found.isPresent());
        Invoice invoice = found.get();
        //Trigger update
        Assertions.assertEquals(Status.NOT_PAID, invoice.getStatus());
        Assertions.assertEquals(0, invoiceListener.getCalled());
        invoice.setStatus(Status.DETECTED);
        invoiceRepository.saveAndFlush(invoice);

        Assertions.assertTrue(output.getOut().contains("NOT_PAID -> DETECTED"));
        Assertions.assertEquals(1, invoiceListener.getCalled());
    }

    //TODO: Test webhook sent on (NOT_PAID -> DETECTED) or (DETECTED -> CONFIRMED) or (DETECTED -> REORG)
}
