package com.example.pyro_cryo.Web3jClient.utils;

import org.web3j.protocol.websocket.events.NewHeadsNotification;

import java.util.List;

public record NotificationWithLogs(NewHeadsNotification notification, List<String> invoiceIds){}
