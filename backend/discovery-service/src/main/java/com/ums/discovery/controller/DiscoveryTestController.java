package com.ums.discovery.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class DiscoveryTestController {

    private final org.springframework.cloud.client.discovery.DiscoveryClient discoveryClient;

    @GetMapping("/test/discovery")
    public Object test() {
        return discoveryClient.getInstances("USER-SERVICE");
    }
}