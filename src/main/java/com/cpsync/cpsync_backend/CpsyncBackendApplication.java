package com.cpsync.cpsync_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CpsyncBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CpsyncBackendApplication.class, args);
	}

}
