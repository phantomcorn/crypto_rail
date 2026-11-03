package com.example.crypto_rail.invoice.unit;

import org.web3j.protocol.websocket.events.NewHeadsNotification;

import java.util.List;

public record NotificationWithLogs(NewHeadsNotification notification, List<String> invoiceIds){}
