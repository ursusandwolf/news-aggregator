package com.moneynews;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MoneyNewsApplication {
    public static void main(String[] args) {
        SpringApplication.run(MoneyNewsApplication.class, args);
    }
}
