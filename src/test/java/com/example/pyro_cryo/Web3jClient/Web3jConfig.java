package com.example.pyro_cryo.Web3jClient;

import com.example.pyro_cryo.utils.Web3jAdapter;
import io.reactivex.processors.PublishProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.web3j.protocol.websocket.events.NewHeadsNotification;

@TestConfiguration
public class Web3jConfig {

    @Bean
    public PublishProcessor<NewHeadsNotification> buildPublisher() {
        return PublishProcessor.create();
    }

    @Bean
    public Web3jAdapter buildWeb3jTestAdapter(PublishProcessor<NewHeadsNotification> publishProcessor) {
        return new Web3jClientTestAdapter(publishProcessor);
    }

}
