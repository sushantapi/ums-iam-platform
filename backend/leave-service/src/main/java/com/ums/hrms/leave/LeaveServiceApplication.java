package com.ums.hrms.leave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

import com.ums.events.config.RabbitMQCommonConfig;
import com.ums.events.publisher.AuditPublisher;

@EnableFeignClients
@Import({AuditPublisher.class, RabbitMQCommonConfig.class})
@SpringBootApplication
public class LeaveServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaveServiceApplication.class, args);
    }
}
