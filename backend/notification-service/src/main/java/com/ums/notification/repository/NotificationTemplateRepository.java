package com.ums.notification.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ums.notification.entity.NotificationTemplate;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

	Optional<NotificationTemplate> findByTemplateCode(String templateCode);

	boolean existsByTemplateCode(String templateCode);
}