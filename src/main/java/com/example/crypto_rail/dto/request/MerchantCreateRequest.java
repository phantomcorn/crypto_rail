package com.example.crypto_rail.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MerchantCreateRequest {

    String name;
    String merchantAddress;
    String uri;

}
