package org.example.seasontonebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;

@SpringBootApplication
public class SeasonToneBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeasonToneBackendApplication.class, args);
    }

}
