package dev.portableagent.action;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ActionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ActionServiceApplication.class, args);
    }
}
