package com.community.community.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisTestRunner implements CommandLineRunner {
    private final StringRedisTemplate redisTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("===============================");
        System.out.println("📡 레디스 연결 테스트 시작...");

        try {
            // 1. 레디스에 키(key) "test:connection", 값(value) "PONG!" 저장
            redisTemplate.opsForValue().set("test:connection", "PONG!");

            // 2. 저장한 값을 다시 꺼내오기
            String value = redisTemplate.opsForValue().get("test:connection");

            System.out.println("🎉 레디스 연결 성공! 읽어온 값: " + value);
        } catch (Exception e) {
            System.out.println("❌ 레디스 연결 실패: " + e.getMessage());
        }
        System.out.println("===============================");
    }
}
