package com.ums.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ums.user.entity.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

	Optional<UserProfile> findByEmail(String email);

	@Query("""
			select u from UserProfile u
			where lower(coalesce(u.email, '')) like concat('%', :search, '%') escape '\\'
			   or lower(coalesce(u.firstName, '')) like concat('%', :search, '%') escape '\\'
			   or lower(coalesce(u.lastName, '')) like concat('%', :search, '%') escape '\\'
			""")
	Page<UserProfile> searchSummaries(@Param("search") String search, Pageable pageable);
}
