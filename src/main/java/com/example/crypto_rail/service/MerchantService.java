package com.example.crypto_rail.service;

import com.example.crypto_rail.dto.MerchantCreateDto;
import com.example.crypto_rail.dto.MerchantDto;
import com.example.crypto_rail.dto.MerchantLoginDto;
import com.example.crypto_rail.entity.Merchant;
import com.example.crypto_rail.repository.MerchantRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class MerchantService implements UserDetailsService {

    @Autowired
    MerchantRepository merchantRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtService jwtService;

    public Optional<MerchantDto> findMerchantByUsername(String username) {
        return merchantRepository.findById(username).map(Merchant::toDto);
    }

    public MerchantDto create(MerchantCreateDto merchantCreateDto) {

        if (merchantRepository.existsById(merchantCreateDto.getMerchantAddress())) {
            return new MerchantDto(null, null, null, null, null, null);
        }

        String key = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());

        Merchant merchant = new Merchant(
                merchantCreateDto.getUsername(),
                encoder.encode(merchantCreateDto.getPassword()),
                merchantCreateDto.getMerchantAddress(),
                merchantCreateDto.getName(),
                merchantCreateDto.getUri(),
                key
        );

        return merchantRepository.save(merchant).toDto();
    }

    public MerchantLoginDto authenticate(MerchantLoginDto dto) {

        boolean exist = merchantRepository.existsById(dto.getUsername());
        if (!exist) {
            return null;
        }

        MerchantDto merchant = merchantRepository.findById(dto.getUsername()).get().toDto();
        if (!encoder.matches(dto.getPassword(), merchant.password())) {
            return null;
        }

        dto.setAccessToken(jwtService.createToken(dto.getUsername(), JwtService.ACCESS_TOKEN_TTL));
        dto.setRefreshToken(jwtService.createToken(dto.getUsername(), JwtService.REFRESH_TOKEN_TTL));
        return dto;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return merchantRepository.findByUsername(username);
    }
}
