package com.example.crypto_rail.Web3jClient;

import org.web3j.protocol.websocket.events.NewHeadsNotification;

import java.util.List;

public record NotificationWithLogs(NewHeadsNotification notification, List<String> invoiceIds){}
