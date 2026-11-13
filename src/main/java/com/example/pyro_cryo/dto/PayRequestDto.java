package com.example.pyro_cryo.dto;

import com.example.pyro_cryo.entity.Status;
import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;

@Data
@Builder
public class PayRequestDto {

    private String id;
    private String hashId;
    private BigInteger amount;
    private String merchantAddress;
    private String tokenAddress;
    private Status status;

}
