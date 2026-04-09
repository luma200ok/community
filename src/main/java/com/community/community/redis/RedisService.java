package com.community.community.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

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

    // 💡 1. 특정 키의 숫자를 1씩 증가 (조회수 카운팅용)
    public void increment(String key) {
        redisTemplate.opsForValue().increment(key);
    }

    // 💡 2. 특정 패턴(viewCount::*)을 가진 모든 키 찾아오기 (scan: 논블로킹, keys: 블로킹 O(N) 사용 금지)
    public Set<String> getKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(keys::add);
        }
        return keys;
    }

    // 💡 3. DB 동기화가 끝난 키는 삭제
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
