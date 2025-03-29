package com.rongcredit.bio.match.console;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(scanBasePackages = { "com.rongcredit" }, exclude = { DataSourceAutoConfiguration.class })
public class BioMatchApp {

    public static void main(String[] args) {
        SpringApplication.run(BioMatchApp.class, args);
    }

}
