package com.example.expensechallenge;

import org.springframework.boot.SpringApplication;

public class TestExpenseChallengeApplication {

	public static void main(String[] args) {
		SpringApplication.from(ExpenseChallengeApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
