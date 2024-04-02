package com.cn.tvn.awscopy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.cn.tvn.awscopy.repository")
public class AwscopyApplication {

	public static void main(String[] args) {
		SpringApplication.run(AwscopyApplication.class, args);
	}

}
