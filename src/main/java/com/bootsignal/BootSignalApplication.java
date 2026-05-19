package com.bootsignal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BootSignalApplication {

	public static void main(String[] args) {
		SpringApplication.run(BootSignalApplication.class, args);
	}

}
