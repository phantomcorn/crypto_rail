package com.example.crypto_rail.utils;

import io.reactivex.Flowable;
import lombok.Getter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthSubscribe;
import org.web3j.protocol.websocket.WebSocketClient;
import org.web3j.protocol.websocket.WebSocketService;
import org.web3j.protocol.websocket.events.NewHeadsNotification;

import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

@Getter
@Component
@Profile("!test")
public class Web3jClientAdapter implements Web3jAdapter{

    private Flowable<NewHeadsNotification> notification;

    public Web3jClientAdapter() throws URISyntaxException, ConnectException {
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

        this.notification = webSocketService.subscribe(
                subscribeRequest,
                "eth_unsubscribe",
                NewHeadsNotification.class
        );
    }

    @Override
    public void onError(Throwable err) {
        err.printStackTrace();
    }

    @Override
    public Flowable<NewHeadsNotification> getPublisher() {
        return notification;
    }

}
