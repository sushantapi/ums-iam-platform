package com.ums.auth.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuditService {

	public void log(UUID uuid, String action, String ip, String status) {

		log.info("User={} Action={} IP={} Status={}", uuid, action, ip, status);
	}

	public void logAnonymous(String action, String ip, String email) {

		log.info("Action={} Email={} IP={}", action, email, ip);
	}
}