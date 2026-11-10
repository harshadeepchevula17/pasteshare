package com.pasteshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PasteShareApplication {

    public static void main(String[] args) {
        SpringApplication.run(PasteShareApplication.class, args);
    }
}
