package com.community.community.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    // 키, 값, 만료시간을 넣으면 레디스에 저장
    public void setValues(String key, String value, Duration duration) {
        redisTemplate.opsForValue().set(key, value,duration);
    }

    // 키로 값을 꺼내오는 메서드
    public String getValues(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteValues(String key) {
        redisTemplate.delete(key);
    }
}
