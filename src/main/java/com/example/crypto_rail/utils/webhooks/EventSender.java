package com.example.crypto_rail.utils.webhooks;

import io.vertx.core.json.JsonObject;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

@Component
public class EventSender {


    public void send(String invoiceId, InvoiceEventStatus invoiceEventStatus, String uri) {

        HttpClient httpClient = HttpClient.newBuilder().build();


        HttpRequest request = webhookRequest(invoiceId, invoiceEventStatus, uri);
        CompletableFuture<HttpResponse<String>> response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

    }

    public HttpRequest webhookRequest(String invoiceId, InvoiceEventStatus status, String uri) {
        //compute secret

        JsonObject body = new JsonObject();
        body.put("invoiceId", invoiceId);
        body.put("status", status.name());

        return HttpRequest.newBuilder()
                .uri(URI.create(uri + "/webhook/event"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
    }
}
