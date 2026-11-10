package com.example.crypto_rail.controller;

import com.example.crypto_rail.dto.MerchantCreateDto;
import com.example.crypto_rail.dto.MerchantDto;
import com.example.crypto_rail.dto.MerchantLoginDto;
import com.example.crypto_rail.dto.request.MerchantCreateRequest;
import com.example.crypto_rail.dto.request.MerchantLoginRequest;
import com.example.crypto_rail.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class MerchantController {

    @Autowired
    MerchantService merchantService;

    @PostMapping("/api/merchant/register")
    public ResponseEntity<String> registerMerchant(@RequestBody MerchantCreateRequest merchantCreateRequest) {

        MerchantCreateDto merchantCreateDto = MerchantCreateDto
                .builder()
                .username(merchantCreateRequest.getUsername())
                .password(merchantCreateRequest.getPassword())
                .name(merchantCreateRequest.getName())
                .merchantAddress(merchantCreateRequest.getMerchantAddress())
                .uri(merchantCreateRequest.getUri())
                .build();

        MerchantDto merchantDto = merchantService.create(merchantCreateDto);
        if (merchantDto.address() == null) {
            return ResponseEntity.ok("Duplicate entry");
        }

        return ResponseEntity.ok("Success");

    }

    @PostMapping("/api/merchant/login")
    public ResponseEntity<String> login(@RequestBody MerchantLoginRequest request) {

        MerchantLoginDto dto = new MerchantLoginDto(request.getUsername(), request.getPassword());
        String token = merchantService.authenticate(dto);
        if (token == null) {
            return ResponseEntity.status(401).body("Incorrect credentials");
        }

        return ResponseEntity.ok(token);

    }
}
