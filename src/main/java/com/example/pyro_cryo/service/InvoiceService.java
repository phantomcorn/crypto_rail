package com.example.pyro_cryo.service;

import com.example.pyro_cryo.dto.InvoiceDto;
import com.example.pyro_cryo.dto.PayRequestDto;
import com.example.pyro_cryo.entity.Invoice;
import com.example.pyro_cryo.entity.Status;
import com.example.pyro_cryo.repository.InvoiceRepository;
import com.example.pyro_cryo.dto.response.PayResponse;
import com.example.pyro_cryo.utils.dto.NewSignatureDto;
import com.example.pyro_cryo.utils.Web3jClient;
import com.example.pyro_cryo.utils.webhooks.EventInbox;
import com.example.pyro_cryo.utils.webhooks.EventMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Autowired
    EventInbox inbox;

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

    @Transactional
    public boolean detectPayment(String hashId) {
        return detectPaymentAll(List.of(hashId));
    }

    @Transactional
    public boolean detectPaymentAll(List<String> hashIds) {
        List<Invoice> lockedInvoices = invoiceRepository.lockByStatus(hashIds, Status.NOT_PAID);
        if (lockedInvoices.isEmpty()) {
            return false;
        }

        List<String> lockedIds = lockedInvoices.stream().map(Invoice::getHashId).toList();
        int changes = invoiceRepository.transition(lockedIds, Status.NOT_PAID, Status.DETECTED);

        assert(changes == lockedInvoices.size());
        lockedInvoices.forEach(invoice ->
                inbox.getPublisher().onNext(
                        EventMessage.builder()
                                .invoiceId(invoice.getHashId())
                                .merchantAddress(invoice.getMerchantAddress())
                                .status(Status.DETECTED)
                                .build()
                )
        );
        return true;
    }

    @Transactional
    public boolean toFinality(List<String> hashIds) {
        List<Invoice> lockedInvoices = invoiceRepository.lockByStatus(hashIds, Status.DETECTED);
        if (lockedInvoices.isEmpty()) {
            return false;
        }

        List<String> lockedIds = lockedInvoices.stream().map(Invoice::getHashId).toList();
        int changes = invoiceRepository.transition(lockedIds, Status.DETECTED, Status.CONFIRMED);

        assert(changes == lockedIds.size());
        lockedInvoices.forEach(invoice ->
                inbox.getPublisher().onNext(
                        EventMessage.builder()
                                .invoiceId(invoice.getHashId())
                                .merchantAddress(invoice.getMerchantAddress())
                                .status(Status.CONFIRMED)
                                .build()
                )
        );
        return true;
    }

    @Transactional
    public boolean revert(String droppedHashId) {
        return revertAll(List.of(droppedHashId));
    }

    @Transactional
    public boolean revertAll(List<String> droppedHashIds) {
        List<Invoice> lockedInvoices = invoiceRepository.lockByStatus(droppedHashIds, Status.DETECTED);
        if (lockedInvoices.isEmpty()) {
            return false;
        }

        List<String> lockedIds = lockedInvoices.stream().map(Invoice::getHashId).toList();
        int changes = invoiceRepository.transition(lockedIds, Status.DETECTED, Status.REORG);

        assert(changes == lockedInvoices.size());
        lockedInvoices.forEach(invoice ->
                inbox.getPublisher().onNext(
                        EventMessage.builder()
                                .invoiceId(invoice.getHashId())
                                .merchantAddress(invoice.getMerchantAddress())
                                .status(Status.REORG)
                                .build()
                )
        );
        return true;
    }
}
