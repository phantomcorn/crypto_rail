package com.example.pyro_cryo.controller;

import com.example.pyro_cryo.dto.response.WatcherHealthResponse;
import com.example.pyro_cryo.service.WatcherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigInteger;

@Controller
public class WatcherController {

    @Autowired
    WatcherService watcherService;

    @GetMapping("/api/watcher/health")
    public ResponseEntity<WatcherHealthResponse> healthCheck() {
        WatcherHealthResponse res;
        BigInteger blockNumber = watcherService.getLastBlockSeen();

        if (blockNumber == null) {
            res = new WatcherHealthResponse(false, BigInteger.ZERO);
        } else {
            res = new WatcherHealthResponse(true, blockNumber);
        }

        return ResponseEntity.ok().body(res);
    }

}
