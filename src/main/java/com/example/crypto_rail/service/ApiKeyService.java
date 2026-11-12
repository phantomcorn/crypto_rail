package com.example.crypto_rail.service;

import com.example.crypto_rail.dto.ApiKeyCreateDto;
import com.example.crypto_rail.dto.ApiKeyDto;
import com.example.crypto_rail.entity.ApiKey;
import com.example.crypto_rail.entity.Merchant;
import com.example.crypto_rail.repository.ApiKeyRepository;
import com.example.crypto_rail.repository.MerchantRepository;
import com.example.crypto_rail.utils.ApiKeyGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;

import java.util.List;
import java.util.Optional;

@Service
public class ApiKeyService {

    @Autowired
    ApiKeyRepository apiKeyRepository;

    @Autowired
    MerchantRepository merchantRepository;

    @Autowired
    ApiKeyGenerator generator;

    public String createKey(ApiKeyCreateDto dto) {

        Merchant merchant = merchantRepository.getReferenceById(dto.username());

        String keyVal = generator.generate(32);

        ApiKey apiKey = ApiKey.builder()
                .label(dto.label())
                .merchant(merchant)
                .hashApiKey(Hash.sha3(keyVal))
                .build();

        apiKeyRepository.save(apiKey);

        return keyVal;
    }

    public ApiKeyDto verify(String username, String rawKey) {
        List<ApiKeyDto> keys = apiKeyRepository.findByMerchantUsername(username).stream().map(ApiKey::toDto).toList();

        Optional<ApiKeyDto> found = keys.stream().filter(dto -> dto.hashPassword().equals(Hash.sha3(rawKey))).findAny();
        return found.orElse(null);

    }
}
