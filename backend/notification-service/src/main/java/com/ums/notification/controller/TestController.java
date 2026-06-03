package com.ums.notification.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.notification.service.EmailService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

	private final EmailService emailService;

	@GetMapping("/welcome")
	public String sendMail() {

		System.out.println("=== WELCOME ENDPOINT HIT ===");

		emailService.sendWelcomeEmail("sushant843120@gmail.com", "Sushant");

		return "Email Sent";
	}
}