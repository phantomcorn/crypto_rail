package com.example.crypto_rail.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class Web3Config {

    @Bean
    public Credentials myCredentials(@Value("${sepolia.private-key}") String privateKey) {
        return Credentials.create(privateKey);
    }

    @Bean
    public Web3j web3j(@Value("${rpc.endpoint-url}") String url) {
        return Web3j.build(new HttpService(url));
    }
}
