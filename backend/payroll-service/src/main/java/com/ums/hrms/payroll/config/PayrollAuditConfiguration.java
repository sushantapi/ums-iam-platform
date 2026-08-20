package com.ums.hrms.payroll.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.ums.events.config.RabbitMQCommonConfig;
import com.ums.events.publisher.AuditPublisher;

@Configuration(proxyBeanMethods = false)
@Import({AuditPublisher.class, RabbitMQCommonConfig.class})
public class PayrollAuditConfiguration {
}
