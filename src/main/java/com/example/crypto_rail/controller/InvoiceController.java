package com.example.crypto_rail.controller;

import com.example.crypto_rail.dto.PayRequestDto;
import com.example.crypto_rail.entity.Invoice;
import com.example.crypto_rail.request.PayRequest;
import com.example.crypto_rail.response.PayResponse;
import com.example.crypto_rail.service.InvoiceService;
import com.example.crypto_rail.web3j.Web3jClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;

@Controller
public class InvoiceController {

    public record MessageView(String invoiceId, String hashId, String tokenAddress, String amount, String merchantAddress){};
    public record DomainView(String name, String version, String chainId, String verifyingContract){};

    @Autowired
    InvoiceService invoiceService;

    @PostMapping("/api/pay")
    public ResponseEntity<PayResponse> pay(@RequestBody PayRequest request) throws IOException {

        PayRequestDto dto = PayRequestDto.builder()
                .amount(request.getAmount())
                .merchantAddress(request.getMerchantAddress())
                .tokenAddress(request.getTokenAddress())
                .build();


        PayResponse resp = invoiceService.createInvoice(dto);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/api/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("hello");
    }

    @GetMapping("/checkout/{invoiceId}")
    public String renderCheckoutPage(Model model, @PathVariable String invoiceId) {

        Optional<Invoice> invoiceObj = invoiceService.getInvoiceDetails(invoiceId);
        if (invoiceObj.isPresent()) {
            Invoice invoice = invoiceObj.get();

            model.addAttribute("domain", new DomainView(
                    invoice.getName(),
                    invoice.getVersion(),
                    invoice.getChainId().toString(),
                    Web3jClient.CRYPTO_RAIL_ADDRESS
            ));

            model.addAttribute("message", new MessageView(
                    invoice.getId(),
                    invoice.getHashId(),
                    invoice.getTokenAddress(),
                    invoice.getAmount().toString(),
                    invoice.getMerchantAddress()
            ));

            model.addAttribute("voucher", invoice.getSignature());
            model.addAttribute("deadline", invoice.getSignatureExpiresAt());
        }

        return "index.html"; //src/main/resources/templates/index.html (Thymeleaf default path)
    }
}
