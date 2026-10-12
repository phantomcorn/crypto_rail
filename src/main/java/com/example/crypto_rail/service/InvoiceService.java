package com.example.crypto_rail.service;

import com.example.crypto_rail.dto.PayRequestDto;
import com.example.crypto_rail.entity.Invoice;
import com.example.crypto_rail.repository.InvoiceRepository;
import com.example.crypto_rail.web3j.Web3jClient;
import org.springframework.beans.factory.annotation.Autowired;
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

    public String createInvoice(PayRequestDto dto) throws IOException {


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

        dto.setStatus(newInvoice.getStatus());

        //create voucher (signature + data)
        String voucher = web3jClient.signTypedMesssge(dto);
        return voucher;
    }
}
