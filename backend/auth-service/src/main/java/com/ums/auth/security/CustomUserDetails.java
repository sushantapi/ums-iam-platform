package com.ums.auth.security;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.ums.auth.entity.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final User user;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {

		// Roles & permissions now come from JWT
		return Collections.emptyList();
	}

	@Override
	public String getPassword() {

		return user.getPasswordHash();
	}

	@Override
	public String getUsername() {

		return user.getEmail();
	}

	@Override
	public boolean isAccountNonExpired() {

		return true;
	}

	@Override
	public boolean isAccountNonLocked() {

		return user.getAccountNonLocked();
	}

	@Override
	public boolean isCredentialsNonExpired() {

		return true;
	}

	@Override
	public boolean isEnabled() {

		return user.getEnabled();
	}

	public UUID getUserId() {

		return user.getId();
	}
}