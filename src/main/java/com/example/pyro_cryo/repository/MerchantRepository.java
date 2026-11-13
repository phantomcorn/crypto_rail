package com.example.pyro_cryo.repository;

import com.example.pyro_cryo.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, String> {

    UserDetails findByUsername(String username);

}
