package com.example.crypto_rail.entity;

import com.example.crypto_rail.dto.ApiKeyDto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Table(name = "api_keys")
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class ApiKey {

    @Id
    @Column(name = "hash_key")
    String hashApiKey;

    @Column(name = "label")
    String label;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "merchant_username", referencedColumnName = "username")
    Merchant merchant;

    public ApiKeyDto toDto() {
        return new ApiKeyDto(label, hashApiKey, merchant);
    }
}
