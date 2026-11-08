package com.example.crypto_rail.dto.request;

import lombok.Data;

@Data
public class MerchantCreateRequest {

    String name;
    String merchantAddress;
    String uri;

}
