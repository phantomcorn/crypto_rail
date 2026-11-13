package com.example.pyro_cryo.repository;

import com.example.pyro_cryo.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {
    List<ApiKey> findByMerchantUsername(String username);
}
