package com.example.crypto_rail.service;

import com.example.crypto_rail.dto.PayRequestDto;
import com.example.crypto_rail.entity.Invoice;
import com.example.crypto_rail.repository.InvoiceRepository;
import com.example.crypto_rail.response.PayResponse;
import com.example.crypto_rail.web3j.NewSignature;
import com.example.crypto_rail.web3j.Web3jClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvoiceService {

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    Web3jClient web3jClient;

    @Value("${checkout-base-url}")
    private String CHECKOUT_BASE_URL;

    public PayResponse createInvoice(PayRequestDto dto) throws IOException {

        String uuid = UUID.randomUUID().toString();
        Invoice newInvoice = invoiceRepository.save(
                Invoice.builder()
                        .id(uuid)
                        .hashId(Hash.sha3String(uuid))
                        .merchantAddress(dto.getMerchantAddress())
                        .tokenAddress(dto.getTokenAddress())
                        .amount(dto.getAmount())
                        .name(Web3jClient.APP_NAME)
                        .version(Web3jClient.APP_VERSION)
                        .chainId(Web3jClient.CHAIN_ID)
                        .decimal(dto.getDecimal())
                        .build()
        );

        dto.setHashId(newInvoice.getHashId());
        dto.setStatus(newInvoice.getStatus());

        //create voucher (signature + data)
        NewSignature voucher = web3jClient.signTypedMesssge(dto);

        newInvoice.setSignature(voucher.signature());
        newInvoice.setSignatureExpiresAt(voucher.deadline());
        invoiceRepository.save(newInvoice);

        String checkoutUrl = CHECKOUT_BASE_URL + "/checkout/" + dto.getHashId();
        return new PayResponse(checkoutUrl);
    }

    public Optional<Invoice> getInvoiceDetails(String invoiceId) {
        return invoiceRepository.findByHashId(invoiceId);
    }

}
