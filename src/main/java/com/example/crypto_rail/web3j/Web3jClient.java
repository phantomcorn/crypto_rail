package com.example.crypto_rail.web3j;

import com.example.crypto_rail.dto.PayRequestDto;
import io.vertx.core.json.JsonObject;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class Web3jClient {

    private static final BigInteger FIVE_MINUTES = BigInteger.valueOf(500);
    public static final Long CHAIN_ID = 11155111L;
    public static final String APP_NAME = "CryptoRail";
    public static final String CRYPTO_RAIL_ADDRESS = "0x46ACcbc000e0144deE00C4910bCF6841424F8937";
    public static final String APP_VERSION = "0.0.1";

    private final Web3j web3j;

    private final Credentials myCredentials;

    public Web3jClient(Web3j web3j, Credentials credentials) {
        this.web3j = web3j;
        this.myCredentials = credentials;
    }

    public NewSignature signTypedMesssge(PayRequestDto dto) throws IOException {
        String jsonMessageString = Files.readString(Paths.get("src/main/resources/static/typedData.json").toAbsolutePath());
        JsonObject jsonObject = new JsonObject(jsonMessageString);
        BigInteger deadline = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send().getBlock().getTimestamp()
                .add(FIVE_MINUTES);
        jsonObject.getJsonObject("message").put("id", dto.getHashId());
        jsonObject.getJsonObject("message").put("tokenAddr", dto.getTokenAddress());
        jsonObject.getJsonObject("message").put("amount", dto.getAmount());
        jsonObject.getJsonObject("message").put("merchantAddr", dto.getMerchantAddress());
        jsonObject.getJsonObject("message").put("deadline", deadline);

        jsonObject.getJsonObject("domain").put("name", APP_NAME);
        jsonObject.getJsonObject("domain").put("version", APP_VERSION);
        jsonObject.getJsonObject("domain").put("chainId", CHAIN_ID);
        jsonObject.getJsonObject("domain").put("verifyingContract", CRYPTO_RAIL_ADDRESS);

        Sign.SignatureData signatureData = Sign.signTypedData(jsonObject.toString(), myCredentials.getEcKeyPair());
        byte[] r = signatureData.getR();
        byte[] s = signatureData.getS();
        byte[] v = signatureData.getV();

        // Standard 65-byte layout: 32 bytes R, 32 bytes S, 1 byte V
        byte[] signatureBytes = new byte[65];
        System.arraycopy(r, 0, signatureBytes, 0, 32);
        System.arraycopy(s, 0, signatureBytes, 32, 32);
        System.arraycopy(v, 0, signatureBytes, 64, 1);

        return new NewSignature(Numeric.toHexString(signatureBytes), deadline);
    }
}
