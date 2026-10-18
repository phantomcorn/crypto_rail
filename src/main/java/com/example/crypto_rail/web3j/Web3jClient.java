package com.example.crypto_rail.web3j;

import com.example.crypto_rail.dto.PayRequestDto;
import com.example.crypto_rail.service.InvoiceService;
import com.example.crypto_rail.service.ProcessedService;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.vertx.core.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.web3j.protocol.core.methods.response.Log;
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
import java.util.*;

@Service
public class Web3jClient {

    private static final BigInteger FIVE_MINUTES = BigInteger.valueOf(500);
    public static final Long CHAIN_ID = 11155111L;
    public static final String APP_NAME = "CryptoRail";
    public static final String CRYPTO_RAIL_ADDRESS = "0xd0F29Ad5DC15a44df7461D16C125cfd809150F19";
    public static final String APP_VERSION = "0.0.1";
    public static final String EVENT = "PaymentReceived(bytes32)";

    private final Integer BLOCKS_TO_FINALITY = 12;
    private final Deque<ProcessedBlock> processedBlock = new ArrayDeque<>();
    private Disposable disposable;

    @Autowired
    Web3j web3j;

    @Autowired
    InvoiceService invoiceService;

    @Autowired
    ProcessedService processedService;

    public Web3jClient() throws URISyntaxException, ConnectException {
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

        disposable = notification.subscribe(this::handler);
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

        //Find all PaymentReceived(hash_id) emitted in this new block
        for (EthLog.LogResult<?> result : ethLog.getLogs()) {
            if (result instanceof Log) {
                Log log = (Log) result.get();
                List<String> topics = log.getTopics();
                assert topics.size() == 2; //topic0 will always be keccack of function signature, topic1 always be first indexed argument see: https://www.alchemy.com/docs/deep-dive-into-eth_getlogs#deciphering-the-response
                String invoiceHashId = topics.get(1);
                if (!invoiceService.detectPayment(invoiceHashId)) {
                    //log not found invoice id
                }
                //log update status to DETECTED
                processedService.process(invoiceHashId, newBlock.getNumber(), newBlock.getHash());
                System.out.println(invoiceHashId + " -> DETECTED");
            }
        }

        //Find any invoice that can be considered final


        //Mark this block as processed
        processedBlock.addLast(new ProcessedBlock(newBlock.getNumber(), newBlock.getHash()));
    }

    private boolean isReorg(NewHead newHead) {
        // [B] <- [A]
        // Make sure newHead's parent hash points to block B (i.e last processed block)
        if (processedBlock.isEmpty()) {
            return false;
        }
        ProcessedBlock prevBlock = processedBlock.getLast();
        return newHead.getParentHash().equals(prevBlock.blockHash());
    }





    public static NewSignature signTypedMesssge(Web3j web3j, Credentials credentials, PayRequestDto dto) throws IOException {
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

        Sign.SignatureData signatureData = Sign.signTypedData(jsonObject.toString(), credentials.getEcKeyPair());
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
