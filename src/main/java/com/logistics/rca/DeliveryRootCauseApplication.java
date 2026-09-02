package com.logistics.rca;

import com.logistics.rca.ai.AiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiProperties.class)
public class DeliveryRootCauseApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliveryRootCauseApplication.class, args);
    }
}
