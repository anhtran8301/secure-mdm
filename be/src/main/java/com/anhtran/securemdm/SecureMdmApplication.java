package com.anhtran.securemdm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SecureMdmApplication {

  public static void main(String[] args) {
    SpringApplication.run(SecureMdmApplication.class, args);
  }
}
