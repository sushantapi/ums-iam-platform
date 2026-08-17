package com.ums.hrms.attendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

import com.ums.events.config.RabbitMQCommonConfig;
import com.ums.events.publisher.AuditPublisher;

@EnableFeignClients
@Import({AuditPublisher.class, RabbitMQCommonConfig.class})
@SpringBootApplication
public class AttendanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AttendanceServiceApplication.class, args);
    }
}
