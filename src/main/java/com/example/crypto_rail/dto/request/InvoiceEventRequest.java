package com.example.crypto_rail.dto.request;

import com.example.crypto_rail.utils.webhooks.InvoiceEventStatus;
import io.vertx.core.json.JsonObject;
import lombok.Builder;
import lombok.Getter;

import java.net.URI;
import java.net.http.HttpRequest;

@Getter
@Builder
public class InvoiceEventRequest {

    private String invoiceId;
    private InvoiceEventStatus status;
    private String secret; //Signature to verify real sender (key shared between merchant and our server)

    public static HttpRequest createRequest(String invoiceId, InvoiceEventStatus status, String uri) {
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
