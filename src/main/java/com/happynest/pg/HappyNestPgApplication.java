package com.happynest.pg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HappyNestPgApplication {

    public static void main(String[] args) {
        SpringApplication.run(HappyNestPgApplication.class, args);
    }
}
