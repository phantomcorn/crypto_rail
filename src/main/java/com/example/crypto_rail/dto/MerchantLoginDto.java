package com.example.crypto_rail.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Data
@Builder
public class MerchantLoginDto {
    String username;
    String password;
    String accessToken;
    String refreshToken;
}
