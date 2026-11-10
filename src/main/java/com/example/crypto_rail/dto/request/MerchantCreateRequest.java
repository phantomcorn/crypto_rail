package com.example.crypto_rail.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MerchantCreateRequest {

    String username;
    String password;
    String name;
    String merchantAddress;
    String uri;

}
