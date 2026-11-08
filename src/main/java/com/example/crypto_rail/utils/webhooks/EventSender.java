package com.example.crypto_rail.utils.webhooks;

import com.example.crypto_rail.dto.MerchantDto;
import com.example.crypto_rail.utils.HMacUtil;
import io.vertx.core.json.JsonObject;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class EventSender {


    public void send(String invoiceId, InvoiceEventStatus invoiceEventStatus, MerchantDto merchantDto) {
        HttpClient httpClient = HttpClient.newBuilder().build();

        HttpRequest request = webhookRequest(invoiceId, invoiceEventStatus, merchantDto);
        CompletableFuture<HttpResponse<String>> response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpRequest webhookRequest(String invoiceId, InvoiceEventStatus status, MerchantDto merchantDto) {
        JsonObject body = new JsonObject();
        body.put("invoiceId", invoiceId);
        body.put("status", status.name());

        Map<String, Object> jsonPayload = body.getMap();
        String signature = HMacUtil.sign(merchantDto.secretKey(), jsonPayload);
        body.put("signature", signature);

        return HttpRequest.newBuilder()
                .uri(URI.create(merchantDto.uri()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
    }
}
