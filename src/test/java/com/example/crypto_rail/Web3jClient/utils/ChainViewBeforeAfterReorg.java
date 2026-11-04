package com.example.crypto_rail.Web3jClient.utils;

import java.util.List;

public record ChainViewBeforeAfterReorg(List<NotificationWithLogs> before, List<NotificationWithLogs> after) { }
