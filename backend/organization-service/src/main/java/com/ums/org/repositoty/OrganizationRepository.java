package com.ums.org.repositoty;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ums.org.entity.Organization;
import com.ums.org.enums.OrganizationStatus;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

	Optional<Organization> findBySlug(String slug);

	long countByStatus(OrganizationStatus status);

	@Query("""
			select o from Organization o
			where lower(o.name) like concat('%', :search, '%') escape '\\'
			   or lower(o.slug) like concat('%', :search, '%') escape '\\'
			""")
	Page<Organization> search(@Param("search") String search, Pageable pageable);
}