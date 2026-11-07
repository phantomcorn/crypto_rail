package com.example.crypto_rail.utils.webhooks;

import com.example.crypto_rail.dto.MerchantDto;
import com.example.crypto_rail.service.MerchantService;
import io.reactivex.disposables.Disposable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile("!test")
public class EventDispatcher { //subscriber to EventInbox

    @Autowired
    MerchantService merchantService;

    @Autowired
    EventSender sender;

    Disposable disposable;

    public EventDispatcher(EventInbox inbox) {
        disposable = inbox.getPublisher().subscribe(this::handler, this::onError);
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

    public void dispose() {
        disposable.dispose();
    }


}
