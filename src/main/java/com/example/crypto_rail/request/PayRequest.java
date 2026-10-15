package com.example.crypto_rail.request;

import lombok.Data;

import java.math.BigInteger;

@Data
public class PayRequest {

    private BigInteger amount;
    private String merchantAddress;
    private String tokenAddress;

}
