package com.postzen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PostZenApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostZenApplication.class, args);
    }
}
