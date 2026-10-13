package com.example.crypto_rail.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PayResponse {
    String voucher;
    String checkoutUrl;
}
