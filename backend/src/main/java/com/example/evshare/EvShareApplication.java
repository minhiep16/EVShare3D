package com.example.evshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EvShareApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvShareApplication.class, args);
    }
}
