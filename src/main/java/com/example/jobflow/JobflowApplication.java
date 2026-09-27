package com.example.jobflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobflowApplication.class, args);
	}

}