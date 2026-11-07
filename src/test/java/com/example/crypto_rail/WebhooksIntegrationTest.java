package com.example.crypto_rail;

import com.example.crypto_rail.dto.MerchantDto;
import com.example.crypto_rail.entity.Status;
import com.example.crypto_rail.service.MerchantService;
import com.example.crypto_rail.utils.webhooks.EventDispatcher;
import com.example.crypto_rail.utils.webhooks.EventInbox;
import com.example.crypto_rail.utils.webhooks.EventMessage;
import com.example.crypto_rail.utils.webhooks.EventSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.web3j.crypto.Hash;
import java.util.UUID;

@SpringBootTest @ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class WebhooksIntegrationTest {

    @Autowired
    EventInbox inbox;

    @MockitoSpyBean
    MerchantService merchantService;

    @MockitoSpyBean
    EventSender sender;

    EventDispatcher dispatcher;
    String merchantAddress = "0x1111111111111111111111111111111111111111";

    @BeforeEach
    public void buildDispatcher() {
        dispatcher = new EventDispatcher(inbox);
        ReflectionTestUtils.setField(dispatcher, "merchantService", merchantService);
        ReflectionTestUtils.setField(dispatcher, "sender", sender);
    }

    @AfterEach
    public void unsubscribe() {
        dispatcher.dispose();
    }

    @Test
    public void dispatcherCanSeeMessageInbox() {
        EventMessage message = EventMessage
                .builder()
                .merchantAddress("0x1111111111111111111111111111111111111111")
                .invoiceId(Hash.sha3String(UUID.randomUUID().toString()))
                .status(Status.DETECTED)
                .build();

        inbox.getPublisher().onNext(message);
        Mockito.verify(merchantService).findMerchantByAddress(message.getMerchantAddress());
    }

    @Test
    public void dispatcherHandlerSendsRequest() {
        EventMessage message = EventMessage
                .builder()
                .merchantAddress(merchantAddress)
                .invoiceId(Hash.sha3String(UUID.randomUUID().toString()))
                .status(Status.DETECTED)
                .build();

        MerchantDto merchantDto = merchantService.findMerchantByAddress(merchantAddress).get();

        inbox.getPublisher().onNext(message);
        Mockito.verify(sender).send(
                message.getInvoiceId(),
                EventMessage.statusToInvoiceEventStatus(message.getStatus()),
                merchantDto
        );
    }
}
