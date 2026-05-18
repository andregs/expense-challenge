package com.example.expensechallenge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ExpenseChallengeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExpenseChallengeApplication.class, args);
	}

}
