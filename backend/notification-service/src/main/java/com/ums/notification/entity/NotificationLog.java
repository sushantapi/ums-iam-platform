package com.ums.notification.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.ums.notification.enums.NotificationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String eventType;

	private String recipient;

	private String recipientEmail;

	private String subject;

	@Enumerated(EnumType.STRING)
	private NotificationStatus status;

	@Column(columnDefinition = "TEXT")
	private String errorMessage;

	private LocalDateTime sentAt;

	@CreationTimestamp
	private LocalDateTime createdAt;
}