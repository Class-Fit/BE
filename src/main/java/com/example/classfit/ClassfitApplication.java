package com.example.classfit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ClassfitApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClassfitApplication.class, args);
	}

}
