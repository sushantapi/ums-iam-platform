/*
 * package com.ums.auth.entity;
 * 
 * import com.ums.auth.enums.RoleType; import jakarta.persistence.*; import
 * lombok.*;
 * 
 * import java.time.LocalDateTime;
 * 
 * @Entity
 * 
 * @Table(name = "users")
 * 
 * @Getter
 * 
 * @Setter
 * 
 * @Builder
 * 
 * @NoArgsConstructor
 * 
 * @AllArgsConstructor public class User {
 * 
 * @Id
 * 
 * @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 * 
 * @Column(nullable = false) private String firstName;
 * 
 * @Column(nullable = false) private String lastName;
 * 
 * @Column(nullable = false, unique = true) private String email;
 * 
 * @Column(nullable = false) private String password;
 * 
 * @Column(unique = true) private String mobile;
 * 
 * @Enumerated(EnumType.STRING) private RoleType role;
 * 
 * @Builder.Default
 * 
 * @Column(nullable = false) private Boolean enabled = true;
 * 
 * @Builder.Default
 * 
 * @Column(nullable = false) private Boolean accountNonLocked = true;
 * 
 * private LocalDateTime createdAt;
 * 
 * private LocalDateTime updatedAt;
 * 
 * @PrePersist public void prePersist() { createdAt = LocalDateTime.now();
 * updatedAt = LocalDateTime.now(); }
 * 
 * @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); } }
 */

package com.ums.auth.entity;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(unique = true)
	private String phone;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "account_non_locked")
	private Boolean accountNonLocked = true;

	@Column(name = "enabled")
	private Boolean enabled = false;

	@Column(name = "failed_login_attempts")
	private Integer failedLoginAttempts = 0;

	@Column(name = "account_locked_until")
	private LocalDateTime accountLockedUntil;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	@CreationTimestamp
	private LocalDateTime createdAt;

	@UpdateTimestamp
	private LocalDateTime updatedAt;
}