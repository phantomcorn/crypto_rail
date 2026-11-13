package com.example.pyro_cryo.controller;

import com.example.pyro_cryo.dto.MerchantCreateDto;
import com.example.pyro_cryo.dto.MerchantDto;
import com.example.pyro_cryo.dto.MerchantLoginDto;
import com.example.pyro_cryo.dto.request.MerchantCreateRequest;
import com.example.pyro_cryo.dto.request.MerchantLoginRequest;
import com.example.pyro_cryo.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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
    public ResponseEntity login(@RequestBody MerchantLoginRequest request) {

        MerchantLoginDto dto = MerchantLoginDto.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .build();
        MerchantLoginDto dtoWithToken = merchantService.authenticate(dto);
        if (dtoWithToken == null) {
            return ResponseEntity.status(401).body("Incorrect credentials");
        }

        ResponseCookie accessCookie = ResponseCookie.from("access_token", dto.getAccessToken())
                .httpOnly(true)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshs_token", dto.getRefreshToken())
                .httpOnly(true)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString(), refreshCookie.toString())
                .body("Success");

    }
}
