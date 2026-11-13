package com.example.pyro_cryo.utils;

import io.reactivex.Flowable;
import org.web3j.protocol.websocket.events.NewHeadsNotification;

public interface Web3jAdapter {

    void onError(Throwable err);

    Flowable<NewHeadsNotification> getPublisher();

}
