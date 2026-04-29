package com.jhpark.tradecore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TradeCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradeCoreApplication.class, args);
    }
}
