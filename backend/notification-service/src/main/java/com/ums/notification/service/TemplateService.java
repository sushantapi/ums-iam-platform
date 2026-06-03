package com.ums.notification.service;

import java.util.List;
import java.util.Map;

import com.ums.notification.dto.CreateTemplateRequest;
import com.ums.notification.entity.NotificationTemplate;

public interface TemplateService {

	NotificationTemplate createTemplate(CreateTemplateRequest request);

	List<NotificationTemplate> getAllTemplates();

	NotificationTemplate getTemplateByCode(String templateCode);

	String getSubject(String templateCode);

	String buildTemplate(String templateCode, Map<String, Object> variables);
}