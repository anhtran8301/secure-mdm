package com.anhtran.secure_mdm;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Secure MDM API", version = "v1"))
public class SecureMdmApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecureMdmApplication.class, args);
	}

}
