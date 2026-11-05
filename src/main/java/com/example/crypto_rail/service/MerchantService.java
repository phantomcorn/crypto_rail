package com.example.crypto_rail.service;

import com.example.crypto_rail.dto.MerchantDto;
import com.example.crypto_rail.entity.Merchant;
import com.example.crypto_rail.repository.MerchantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MerchantService {

    @Autowired
    MerchantRepository merchantRepository;

    public Optional<MerchantDto> findMerchantByAddress(String address) {
        return merchantRepository.findById(address).map(Merchant::toDto);
    }

}
