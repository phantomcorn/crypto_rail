package com.example.crypto_rail.controller;

import com.example.crypto_rail.dto.PayRequestDto;
import com.example.crypto_rail.request.PayRequest;
import com.example.crypto_rail.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;

@Controller
public class InvoiceController {

    @Autowired
    InvoiceService merchantService;

    @PostMapping("/api/pay")
    public ResponseEntity pay(@RequestBody PayRequest request) throws IOException {

        PayRequestDto dto = PayRequestDto.builder()
                .amount(request.getAmount())
                .decimal(request.getDecimal())
                .merchantAddress(request.getMerchantAddress())
                .build();

        merchantService.createInvoice(dto);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("hello");
    }
}
