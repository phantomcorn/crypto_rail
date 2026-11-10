package com.example.crypto_rail.entity;

import com.example.crypto_rail.dto.MerchantDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "merchant")
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Merchant implements UserDetails {

    @Id
    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "address", length=42) //Address is 20 bytes. In hex that is 40 character + "0x" = 42
    private String address;

    @Column(name = "name")
    private String name;

    @Column(name = "uri")
    private String uri;

    @Column(name = "secret_key")
    private String secretKey; //signing webhooks

    public MerchantDto toDto() {
        return new MerchantDto(username, password, address, name, uri, secretKey);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }
}
