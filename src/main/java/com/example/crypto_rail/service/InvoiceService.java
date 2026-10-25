package com.example.crypto_rail.service;

import com.example.crypto_rail.dto.InvoiceDto;
import com.example.crypto_rail.dto.PayRequestDto;
import com.example.crypto_rail.entity.Invoice;
import com.example.crypto_rail.entity.Status;
import com.example.crypto_rail.repository.InvoiceRepository;
import com.example.crypto_rail.response.PayResponse;
import com.example.crypto_rail.utils.dto.NewSignatureDto;
import com.example.crypto_rail.utils.Web3jClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvoiceService {

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    Web3j web3j;

    @Autowired
    Credentials credentials;

    @Value("${checkout-base-url}")
    private String CHECKOUT_BASE_URL;

    public PayResponse createInvoice(PayRequestDto dto) throws IOException {

        String uuid = UUID.randomUUID().toString();
        dto.setHashId(Hash.sha3String(uuid));
        dto.setStatus(Status.NOT_PAID);
        Invoice newInvoice = Invoice.builder()
                        .id(uuid)
                        .hashId(dto.getHashId())
                        .merchantAddress(dto.getMerchantAddress())
                        .tokenAddress(dto.getTokenAddress())
                        .amount(dto.getAmount())
                        .name(Web3jClient.APP_NAME)
                        .version(Web3jClient.APP_VERSION)
                        .chainId(Web3jClient.CHAIN_ID)
                        .status(dto.getStatus())
                        .build();

        //create voucher (signature + data)
        NewSignatureDto voucher = Web3jClient.signTypedMesssge(web3j, credentials, dto);

        newInvoice.setSignature(voucher.signature());
        newInvoice.setSignatureExpiresAt(voucher.deadline());
        invoiceRepository.save(newInvoice);

        String checkoutUrl = CHECKOUT_BASE_URL + "/checkout/" + dto.getHashId();
        return new PayResponse(checkoutUrl, newInvoice.toDto());
    }

    public Optional<InvoiceDto> getInvoiceDetails(String invoiceId) {
        Optional<Invoice> found = invoiceRepository.findByHashId(invoiceId);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(found.get().toDto());
    }

    public boolean detectPayment(String hashId) {
        Optional<Invoice> found = invoiceRepository.findByHashId(hashId);
        if (found.isEmpty()) {
            return false;
        }
        Invoice invoice = found.get();
        if (!invoice.getStatus().equals(Status.NOT_PAID)) {
            return false;
        }

        invoice.setStatus(Status.DETECTED);
        invoiceRepository.save(invoice);
        return true;
    }

    public boolean detectPaymentAll(List<String> hashIds) {
        List<Invoice> invoices = invoiceRepository.findAllByHashIdIn(hashIds);
        List<Invoice> detectedInvoice = invoices.stream().filter(invoice -> invoice.getStatus().equals(Status.NOT_PAID))
                .peek(invoice -> invoice.setStatus(Status.DETECTED))
                .toList();

        if (detectedInvoice.isEmpty()) {
            return false;
        }
        invoiceRepository.saveAllAndFlush(detectedInvoice);
        return true;
    }

    public boolean toFinality(List<String> hashIds) {
        List<Invoice> invoiceToFinalize = invoiceRepository.findAllByHashIdIn(hashIds);

        //only update ones that are in DETECTED
        List<Invoice> unconfirmedInvoice = invoiceToFinalize
                .stream().filter(invoice -> invoice.getStatus().equals(Status.DETECTED))
                .peek(invoice -> invoice.setStatus(Status.CONFIRMED))
                .toList();
        if (unconfirmedInvoice.isEmpty()) {
            return false;
        }
        invoiceRepository.saveAllAndFlush(unconfirmedInvoice);
        return true;
    }

    public boolean revert(String droppedHashId) {
        Optional<Invoice> found = invoiceRepository.findByHashId(droppedHashId);
        if (found.isEmpty()) {
            return false;
        }
        Invoice invoice = found.get();
        if (!invoice.getStatus().equals(Status.CONFIRMED)) {
            return false;
        }
        invoice.setStatus(Status.REORG);
        invoiceRepository.save(invoice);
        return true;
    }

    public boolean revertAll(List<String> droppedHashIds) {
        List<Invoice> foundInvoice = invoiceRepository.findAllByHashIdIn(droppedHashIds);
        List<Invoice> revertedInvoices = foundInvoice
                .stream().filter(invoice -> invoice.getStatus().equals(Status.CONFIRMED))
                .peek(invoice -> invoice.setStatus(Status.REORG))
                .toList();
        if (revertedInvoices.isEmpty()) {
            return false;
        }
        invoiceRepository.saveAllAndFlush(revertedInvoices);
        return true;
    }
}
