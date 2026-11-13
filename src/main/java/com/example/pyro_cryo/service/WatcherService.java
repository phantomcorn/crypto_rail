package com.example.pyro_cryo.service;

import com.example.pyro_cryo.utils.Web3jClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.io.IOException;
import java.math.BigInteger;

@Service
public class WatcherService {

    @Autowired
    Web3j web3j;

    public BigInteger getLastBlockSeen() {
        try {
            EthBlock ethBlock = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send();
            return ethBlock.getBlock().getNumber();
        } catch (IOException e) {
            return null;
        }
    }
}
