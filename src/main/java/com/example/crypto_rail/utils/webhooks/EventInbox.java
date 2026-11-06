package com.example.crypto_rail.utils.webhooks;

import io.reactivex.processors.PublishProcessor;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
public class EventInbox {

    @Getter
    private final PublishProcessor<EventMessage> publisher;

    public EventInbox() {
        publisher = PublishProcessor.create();
    }

}
