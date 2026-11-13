package com.example.pyro_cryo.dto;

import com.example.pyro_cryo.entity.Merchant;

public record ApiKeyDto(String label, String hashPassword, Merchant merchant) {
}
