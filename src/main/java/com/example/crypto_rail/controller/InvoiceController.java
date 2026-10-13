package com.example.crypto_rail.controller;

import com.example.crypto_rail.dto.PayRequestDto;
import com.example.crypto_rail.entity.Invoice;
import com.example.crypto_rail.request.PayRequest;
import com.example.crypto_rail.response.PayResponse;
import com.example.crypto_rail.service.InvoiceService;
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

    @Autowired
    InvoiceService invoiceService;

    @PostMapping("/api/pay")
    public ResponseEntity<PayResponse> pay(@RequestBody PayRequest request) throws IOException {

        PayRequestDto dto = PayRequestDto.builder()
                .amount(request.getAmount())
                .decimal(request.getDecimal())
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
            model.addAttribute("invoiceId", invoice.getId());
            model.addAttribute("hashId", invoice.getHashId());
            model.addAttribute("tokenAddress", invoice.getTokenAddress());
            if (invoice.getDecimal() > 0) {
                model.addAttribute("amount", invoice.getAmount().divide(BigInteger.valueOf(invoice.getDecimal())));
            } else {
                model.addAttribute("amount", invoice.getAmount());
            }
            model.addAttribute("merchantAddress", invoice.getMerchantAddress());
        }


        return "index.html"; //src/main/resources/templates/index.html (Thymeleaf default path)
    }
}
