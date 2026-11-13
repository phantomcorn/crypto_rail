package com.example.pyro_cryo.controller;

import com.example.pyro_cryo.dto.ApiKeyCreateDto;
import com.example.pyro_cryo.dto.request.ApiKeyCreateRequest;
import com.example.pyro_cryo.service.ApiKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class ApiKeyController {

    @Autowired
    ApiKeyService apiKeyService;

    @PostMapping("/api/key/create")
    public ResponseEntity<String> generateKey(@RequestBody ApiKeyCreateRequest request, @AuthenticationPrincipal UserDetails userDetails) {

        ApiKeyCreateDto dto = new ApiKeyCreateDto(request.getLabel(), userDetails.getUsername());

        return ResponseEntity.ok(apiKeyService.createKey(dto));
    }

}
