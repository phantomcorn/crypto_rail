package com.example.crypto_rail.utils.webhooks;

import com.example.crypto_rail.dto.MerchantDto;
import com.example.crypto_rail.dto.request.InvoiceEventRequest;
import com.example.crypto_rail.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EventDispatcher { //subscriber to EventInbox

    @Autowired
    MerchantService merchantService;

    @Autowired
    EventSender sender;
    public EventDispatcher(EventInbox inbox) {
        inbox.getPublisher().subscribe(this::handler, this::onError);
    }

    public void handler(EventMessage message) {
        //find merchant uri
        Optional<MerchantDto> found = merchantService.findMerchantByAddress(message.merchantAddress);
        if (found.isEmpty()) {
            //TODO: raise error (this handler should only be called when event updates i.e must have merchant entity)
        }
        MerchantDto merchant = found.get();
        sender.send(message.invoiceId, EventMessage.statusToInvoiceEventStatus(message.status), merchant.uri());
    }

    public void onError(Throwable err) {
        err.printStackTrace();
    }


}
