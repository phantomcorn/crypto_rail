package com.example.crypto_rail.utils;

import com.example.crypto_rail.entity.Invoice;
import com.example.crypto_rail.entity.Status;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.Getter;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InvoiceListener implements PostUpdateEventListener {

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Getter
    Integer called = 0; //debug purpose

    @PostConstruct
    public void registerListener() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry listenerRegistry = sessionFactory.getEventListenerRegistry();

        listenerRegistry.appendListeners(EventType.POST_UPDATE, this);
    }

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
        called+=1;
    }

}
