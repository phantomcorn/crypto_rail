package com.example.crypto_rail.dto;

import com.example.crypto_rail.entity.Merchant;

public record ApiKeyDto(String label, String hashPassword, Merchant merchant) {
}
