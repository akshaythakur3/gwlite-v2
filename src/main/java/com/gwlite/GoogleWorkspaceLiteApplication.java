package com.gwlite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // required for DocumentSnapshotScheduler's @Scheduled job (#4)
public class GoogleWorkspaceLiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoogleWorkspaceLiteApplication.class, args);
    }

}
