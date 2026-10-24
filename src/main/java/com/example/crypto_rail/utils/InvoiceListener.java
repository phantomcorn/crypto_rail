package com.example.crypto_rail.utils;

import com.example.crypto_rail.entity.Invoice;
import com.example.crypto_rail.entity.Status;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;

public class InvoiceListener implements PostUpdateEventListener {

    @Override
    public void onPostUpdate(PostUpdateEvent event) {

        Object entity = event.getEntity();
        if (entity instanceof Invoice) {

            String[] property = event.getPersister().getPropertyNames();
            for (int index: event.getDirtyProperties()) {
                if (!property[index].equals("status")) {
                    continue;
                }

                Status oldStatus = (Status) event.getOldState()[index];
                Status newStatus = (Status) event.getState()[index];

                System.out.println(oldStatus + " -> " + newStatus);
            }
        }
    }
}
