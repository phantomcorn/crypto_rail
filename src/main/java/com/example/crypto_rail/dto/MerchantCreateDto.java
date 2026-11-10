package com.example.crypto_rail.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MerchantCreateDto {

    String username;
    String password;
    String name;
    String merchantAddress;
    String uri;
}
