package com.example.crypto_rail.utils.webhooks;

import com.example.crypto_rail.dto.MerchantDto;
import com.example.crypto_rail.dto.request.InvoiceEventRequest;
import com.example.crypto_rail.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class EventDispatcher { //subscriber to EventInbox

    @Autowired
    MerchantService merchantService;

    public EventDispatcher(EventInbox inbox) {
        inbox.getPublisher().subscribe(this::handler, this::onError);
    }

    public void handler(EventMessage message) {
        Optional<MerchantDto> found = merchantService.findMerchantByAddress(message.merchantAddress);
        if (found.isEmpty()) {
            //TODO: raise error (this handler should only be called when event updates i.e must have merchant entity)
        }
        MerchantDto merchant = found.get();

        //find merchant uri
        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpRequest request = InvoiceEventRequest.createRequest(message.invoiceId, EventMessage.statusToInvoiceEventStatus(message.status), merchant.uri());

        CompletableFuture<HttpResponse<String>> response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public void onError(Throwable err) {
        err.printStackTrace();
    }


}
