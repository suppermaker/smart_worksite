package com.xd.smartworksite;

import com.xd.smartworksite.auth.application.LoginSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(LoginSecurityProperties.class)
public class SmartWorksiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartWorksiteApplication.class, args);
    }
}
