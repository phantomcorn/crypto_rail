package com.example.pyro_cryo.utils.webhooks;

import com.example.pyro_cryo.entity.Status;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class EventMessage {

    String username;
    String merchantAddress;
    String invoiceId;
    Status status;

    public static InvoiceEventStatus statusToInvoiceEventStatus(Status status) {

        InvoiceEventStatus res = null;

        switch (status) {
            case NOT_PAID, DETECTED -> res = InvoiceEventStatus.PENDING;
            case REORG -> res = InvoiceEventStatus.REVERTED;
            case CONFIRMED -> res = InvoiceEventStatus.PAID;
        }
        return res;
    }
}
