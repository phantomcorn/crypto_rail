package com.example.pyro_cryo.service;

import com.example.pyro_cryo.dto.ApiKeyCreateDto;
import com.example.pyro_cryo.dto.ApiKeyDto;
import com.example.pyro_cryo.entity.ApiKey;
import com.example.pyro_cryo.entity.Merchant;
import com.example.pyro_cryo.repository.ApiKeyRepository;
import com.example.pyro_cryo.repository.MerchantRepository;
import com.example.pyro_cryo.utils.ApiKeyGenerator;
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
