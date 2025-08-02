package com.equities;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.CrossOrigin;

@SpringBootApplication
@EnableAsync
@CrossOrigin(origins = "*")
public class EquitiesApplication {

	public static void main(String[] args) {
		SpringApplication.run(EquitiesApplication.class, args);
	}

}
