package com.ums.hrms.employee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

import com.ums.events.config.RabbitMQCommonConfig;
import com.ums.events.publisher.AuditPublisher;

@EnableFeignClients
@Import({AuditPublisher.class, RabbitMQCommonConfig.class})
@SpringBootApplication
public class EmployeeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeServiceApplication.class, args);
    }
}
