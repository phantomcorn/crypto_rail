package com.example.crypto_rail.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigInteger;

@Data
@AllArgsConstructor
public class PayRequest {

    private BigInteger amount;
    private String merchantAddress;
    private String tokenAddress;

}
