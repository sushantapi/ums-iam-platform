package com.ums.notification.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

import com.ums.notification.service.EmailService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Profile("dev")
public class TestController {

	private final EmailService emailService;

	@GetMapping("/welcome")
	public String sendMail() {

		emailService.sendWelcomeEmail("demo.user@example.test", "Demo User");

		return "Email Sent";
	}
}
