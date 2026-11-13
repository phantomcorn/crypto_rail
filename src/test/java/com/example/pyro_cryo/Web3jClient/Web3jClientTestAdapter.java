package com.example.pyro_cryo.Web3jClient;

import com.example.pyro_cryo.utils.Web3jAdapter;
import io.reactivex.Flowable;
import io.reactivex.processors.PublishProcessor;
import org.web3j.protocol.websocket.events.NewHeadsNotification;

public class Web3jClientTestAdapter implements Web3jAdapter {

    private final Flowable<NewHeadsNotification> notification;

    public Web3jClientTestAdapter(PublishProcessor<NewHeadsNotification> newHeadPublisher) {
        this.notification = newHeadPublisher;
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
