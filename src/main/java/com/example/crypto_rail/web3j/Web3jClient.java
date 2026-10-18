package com.example.crypto_rail.web3j;

import com.example.crypto_rail.dto.PayRequestDto;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.vertx.core.json.JsonObject;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.EthSubscribe;
import org.web3j.protocol.websocket.WebSocketClient;
import org.web3j.protocol.websocket.WebSocketService;
import org.web3j.protocol.websocket.events.NewHead;
import org.web3j.protocol.websocket.events.NewHeadsNotification;
import org.web3j.utils.Bloom;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

@Service
public class Web3jClient {

    private static final BigInteger FIVE_MINUTES = BigInteger.valueOf(500);
    public static final Long CHAIN_ID = 11155111L;
    public static final String APP_NAME = "CryptoRail";
    public static final String CRYPTO_RAIL_ADDRESS = "0xd361aDb138F0fc54b127d5c1d14ce78Cf4EbFc09";
    public static final String APP_VERSION = "0.0.1";
    public static final String EVENT = "PaymentReceived(bytes32)";

    private final Web3j web3j;

    private final Credentials myCredentials;

    public Web3jClient(Web3j web3j, Credentials credentials) throws URISyntaxException, ConnectException {
        this.web3j = web3j;
        this.myCredentials = credentials;

        WebSocketClient webSocketClient = new WebSocketClient(new URI("wss://eth-sepolia.g.alchemy.com/v2/alch_dC-N1oQkucc4aRmNyBCf9"));
        WebSocketService webSocketService = new WebSocketService(webSocketClient, false);
        webSocketService.connect();

        if (!webSocketClient.isOpen()) {
            return;
        }

        Request<?, EthSubscribe> subscribeRequest = new Request<>(
                "eth_subscribe",
                Arrays.asList("newHeads", Collections.emptyList()),
                webSocketService,
                EthSubscribe.class
        );

        Flowable<NewHeadsNotification> notification = webSocketService.subscribe(
                subscribeRequest,
                "eth_unsubscribe",
                NewHeadsNotification.class
        );

        Disposable disposable = notification.subscribe(this::handler);


    }

    private void handler(NewHeadsNotification notification) throws IOException {
        NewHead newBlock = notification.getParams().getResult();

        String topic0 = Numeric.toHexString(Hash.sha3(EVENT).getBytes());
        boolean exists = Bloom.test(newBlock.getLogsBloom(), CRYPTO_RAIL_ADDRESS, topic0);
        if (!exists) {
            return;
        }

        EthLog ethLog = web3j.ethGetLogs(
                new EthFilter(newBlock.getHash(), CRYPTO_RAIL_ADDRESS).addSingleTopic(topic0)
        ).send();

        for (EthLog.LogResult result : ethLog.getLogs()) {

        }
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
