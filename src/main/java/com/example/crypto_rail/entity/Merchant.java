package com.example.crypto_rail.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "merchant")
@Getter @Setter
@Builder
public class Merchant {

    @Id
    @Column(name = "address", length=42) //Address is 20 bytes. In hex that is 40 character + "0x" = 42
    private String address;

    @Column(name = "name")
    private String name;
}
