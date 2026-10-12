package com.example.crypto_rail.dto;

import com.example.crypto_rail.entity.Status;
import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;

@Data
@Builder
public class PayRequestDto {

    private String id;
    private BigInteger amount;
    private Integer decimal;
    private String merchantAddress;
    private String tokenAddress;
    private Status status;

}
