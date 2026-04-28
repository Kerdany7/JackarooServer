package com.jackaroo.jackaroo_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JackarooBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(JackarooBackendApplication.class, args);
	}

}
