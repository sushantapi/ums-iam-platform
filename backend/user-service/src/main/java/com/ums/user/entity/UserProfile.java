
package com.ums.user.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

	@Id
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "user_id", length = 36)
	private UUID userId;

	private String firstName;

	private String lastName;

	@Column(unique = true)
	private String email;

	private String mobile;

	private String avatarUrl;

	private String address;

	private String city;

	private String state;

	private String country;

	private String zipCode;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;
}
