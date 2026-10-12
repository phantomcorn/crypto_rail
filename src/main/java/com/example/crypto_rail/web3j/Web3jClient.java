package com.example.crypto_rail.web3j;

import com.example.crypto_rail.dto.PayRequestDto;
import io.vertx.core.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class Web3jClient {

    private static final BigInteger FIVE_MINUTES = BigInteger.valueOf(500);
    private static final BigInteger CHAIN_ID = BigInteger.valueOf(11155111);
    private static final String CRYPTO_RAIL_ADDRESS = "";

    private final Web3j web3j;

    private final Credentials myCredentials;

    public Web3jClient(Web3j web3j, Credentials credentials) {
        this.web3j = web3j;
        this.myCredentials = credentials;
    }

    public String signTypedMesssge(PayRequestDto dto) throws IOException {
        String jsonMessageString = Files.readString(Paths.get("src/main/resources/static/typedData.json").toAbsolutePath());
        JsonObject jsonObject = new JsonObject(jsonMessageString);

        jsonObject.getJsonObject("message").put("id", dto.getId());
        jsonObject.getJsonObject("message").put("tokenAddr", dto.getTokenAddress());
        jsonObject.getJsonObject("message").put("amount", dto.getAmount());
        jsonObject.getJsonObject("message").put("merchantAddr", dto.getMerchantAddress());
        jsonObject.getJsonObject("message").put("deadline",
                web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send().getBlock().getTimestamp()
                        .add(FIVE_MINUTES));

        jsonObject.getJsonObject("domain").put("chainId", CHAIN_ID);
        jsonObject.getJsonObject("domain").put("verifyingContract", CRYPTO_RAIL_ADDRESS);


        return Sign.signTypedData(jsonObject.toString(), myCredentials.getEcKeyPair()).toString();
    }
}
