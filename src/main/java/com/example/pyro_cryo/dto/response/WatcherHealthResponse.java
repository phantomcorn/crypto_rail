package com.example.pyro_cryo.dto.response;

import java.math.BigInteger;

public record WatcherHealthResponse(boolean online, BigInteger blockNumber) { }
