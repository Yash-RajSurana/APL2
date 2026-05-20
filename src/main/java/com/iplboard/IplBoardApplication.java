package com.iplboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IplBoardApplication {
    public static void main(String[] args) {
        SpringApplication.run(IplBoardApplication.class, args);
    }
}
