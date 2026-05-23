package org.example.demo19.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class WebFrontendApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebFrontendApplication.class, args);
    }
}
