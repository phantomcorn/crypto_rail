package com.example.crypto_rail.repository;

import com.example.crypto_rail.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, String> {

    UserDetails findByUsername(String username);

}
