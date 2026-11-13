package com.example.pyro_cryo.Web3jClient.utils;

import java.util.List;

public record ChainViewBeforeAfterReorg(List<NotificationWithLogs> before, List<NotificationWithLogs> after) { }
