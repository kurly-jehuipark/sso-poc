package com.kurly.poc.sso.gluu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class GluuPocApplication {

  public static void main(String[] args) {
    SpringApplication.run(GluuPocApplication.class, args);
  }
}
