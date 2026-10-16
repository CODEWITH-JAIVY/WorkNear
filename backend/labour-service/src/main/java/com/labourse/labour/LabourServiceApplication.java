package com.labourse.labour;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class LabourServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LabourServiceApplication.class, args);
    }

    // @LoadBalanced lets us call http://MATCHING-SERVICE/... and Eureka resolves the real host:port
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
