package com.ums.admin.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "session-service", contextId = "sessionClient")
public interface SessionServiceClient {
}