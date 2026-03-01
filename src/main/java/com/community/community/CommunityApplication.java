package com.community.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing // 시간 자동화 기능 활성화
@SpringBootApplication
public class CommunityApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommunityApplication.class, args);
	}

}
