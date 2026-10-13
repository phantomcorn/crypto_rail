package com.example.crypto_rail.service;

import com.example.crypto_rail.dto.PayRequestDto;
import com.example.crypto_rail.entity.Invoice;
import com.example.crypto_rail.repository.InvoiceRepository;
import com.example.crypto_rail.response.PayResponse;
import com.example.crypto_rail.web3j.Web3jClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;

import java.io.IOException;
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
                .hashId(Hash.sha3(uuid))
                .amount(dto.getAmount())
                .decimal(dto.getDecimal())
                .merchantAddress(dto.getMerchantAddress())
                .build()
        );

        dto.setHashId(newInvoice.getHashId());
        dto.setStatus(newInvoice.getStatus());

        //create voucher (signature + data)
        String voucher = web3jClient.signTypedMesssge(dto);
        String checkoutUrl = CHECKOUT_BASE_URL + "/checkout/" + dto.getHashId();
        return PayResponse.builder().voucher(voucher).checkoutUrl(checkoutUrl).build();
    }
}
