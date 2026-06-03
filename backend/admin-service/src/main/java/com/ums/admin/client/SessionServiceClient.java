package com.ums.admin.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "session-service")
public interface SessionServiceClient {

}