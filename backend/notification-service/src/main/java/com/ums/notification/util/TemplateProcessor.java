package com.ums.notification.util;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class TemplateProcessor {

	public String process(String template, Map<String, String> variables) {

		String result = template;

		for (Map.Entry<String, String> entry : variables.entrySet()) {

			result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
		}

		return result;
	}
}