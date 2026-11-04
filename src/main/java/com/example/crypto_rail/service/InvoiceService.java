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
        List<String> lockedIds = invoiceRepository.lockByStatus(hashIds, Status.NOT_PAID);
        if (lockedIds.isEmpty()) {
            return false;
        }

        int changes = invoiceRepository.transition(lockedIds, Status.NOT_PAID, Status.DETECTED);
        //TODO: publish event
        return changes > 0;
    }

    @Transactional
    public boolean toFinality(List<String> hashIds) {
        List<String> lockedIds = invoiceRepository.lockByStatus(hashIds, Status.DETECTED);
        if (lockedIds.isEmpty()) {
            return false;
        }

        int changes = invoiceRepository.transition(lockedIds, Status.DETECTED, Status.CONFIRMED);
        //TODO: publish event
        return changes > 0;
    }

    @Transactional
    public boolean revert(String droppedHashId) {
        return revertAll(List.of(droppedHashId));
    }

    @Transactional
    public boolean revertAll(List<String> droppedHashIds) {
        List<String> lockedIds = invoiceRepository.lockByStatus(droppedHashIds, Status.DETECTED);
        if (lockedIds.isEmpty()) {
            return false;
        }

        int changes = invoiceRepository.transition(lockedIds, Status.DETECTED, Status.REORG);
        //TODO: publish event
        return changes > 0;
    }
}
