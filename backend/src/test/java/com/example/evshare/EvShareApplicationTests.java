package com.example.evshare;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class EvShareApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Should successfully load Spring ApplicationContext with JPA and Flyway")
    void contextLoads() {
        assertNotNull(applicationContext, "ApplicationContext should not be null");
    }
}
