package com.ums.notification.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ums.notification.dto.CreateTemplateRequest;
import com.ums.notification.entity.NotificationTemplate;
import com.ums.notification.exception.TemplateAlreadyExistsException;
import com.ums.notification.exception.TemplateNotFoundException;
import com.ums.notification.repository.NotificationTemplateRepository;
import com.ums.notification.service.TemplateService;
import com.ums.notification.util.TemplateProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TemplateServiceImpl implements TemplateService {

	private final NotificationTemplateRepository templateRepository;
	private final TemplateProcessor templateProcessor;

	@Override
	@Transactional
	public NotificationTemplate createTemplate(CreateTemplateRequest request) {

		log.info("Creating notification template: {}", request.getTemplateCode());

		if (templateRepository.existsByTemplateCode(request.getTemplateCode())) {

			throw new TemplateAlreadyExistsException(request.getTemplateCode());
		}

		NotificationTemplate template = NotificationTemplate.builder().templateCode(request.getTemplateCode())
				.subject(request.getSubject()).body(request.getBody()).channel(request.getChannel()).active(true)
				.build();

		NotificationTemplate saved = templateRepository.save(template);

		log.info("Template created successfully. id={}", saved.getId());

		return saved;
	}

	@Override
	public List<NotificationTemplate> getAllTemplates() {

		log.debug("Fetching all notification templates");

		return templateRepository.findAll();
	}

	@Override
	public NotificationTemplate getTemplateByCode(String templateCode) {

		log.debug("Fetching template: {}", templateCode);

		return templateRepository.findByTemplateCode(templateCode)
				.orElseThrow(() -> new TemplateNotFoundException(templateCode));
	}

	@Override
	public String getSubject(String templateCode) {

		return getTemplateByCode(templateCode).getSubject();
	}

	@Override
	public String buildTemplate(String templateCode, Map<String, Object> variables) {

		NotificationTemplate template = getTemplateByCode(templateCode);

		Map<String, String> values = variables.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> String.valueOf(entry.getValue())));

		String processedTemplate = templateProcessor.process(template.getBody(), values);

		log.debug("Template {} rendered successfully", templateCode);

		return processedTemplate;
	}
}