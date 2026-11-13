package com.example.pyro_cryo;

import com.example.pyro_cryo.dto.MerchantDto;
import com.example.pyro_cryo.entity.Status;
import com.example.pyro_cryo.service.MerchantService;
import com.example.pyro_cryo.utils.webhooks.EventDispatcher;
import com.example.pyro_cryo.utils.webhooks.EventInbox;
import com.example.pyro_cryo.utils.webhooks.EventMessage;
import com.example.pyro_cryo.utils.webhooks.EventSender;
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
    String merchantUsername = "user1";

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
                .username("user1")
                .merchantAddress("0x1111111111111111111111111111111111111111")
                .invoiceId(Hash.sha3String(UUID.randomUUID().toString()))
                .status(Status.DETECTED)
                .build();

        inbox.getPublisher().onNext(message);
        Mockito.verify(merchantService).findMerchantByUsername(message.getUsername());
    }

    @Test
    public void dispatcherHandlerSendsRequest() {
        EventMessage message = EventMessage
                .builder()
                .username(merchantUsername)
                .merchantAddress(merchantAddress)
                .invoiceId(Hash.sha3String(UUID.randomUUID().toString()))
                .status(Status.DETECTED)
                .build();

        MerchantDto merchantDto = merchantService.findMerchantByUsername(merchantUsername).get();

        inbox.getPublisher().onNext(message);
        Mockito.verify(sender).send(
                message.getInvoiceId(),
                EventMessage.statusToInvoiceEventStatus(message.getStatus()),
                merchantDto
        );
    }
}
