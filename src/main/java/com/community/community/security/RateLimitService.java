package com.community.community.security;

import com.community.community.exception.CustomException;
import com.community.community.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    // INCR + 신규 키일 때만 EXPIRE 를 Lua 스크립트로 원자적 처리
    // → increment 와 expire 사이 서버 재시작/단절로 TTL 미설정돼 영구 차단되는 버그 방지
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1])\n" +
            "if tonumber(count) == 1 then\n" +
            "  redis.call('EXPIRE', KEYS[1], ARGV[1])\n" +
            "end\n" +
            "return count",
            Long.class
    );

    public void checkLoginRateLimit(String ip) {
        check("rate:login:" + ip, 10, Duration.ofMinutes(5));
    }

    public void checkSignupRateLimit(String ip) {
        check("rate:signup:" + ip, 5, Duration.ofHours(1));
    }

    public void checkPasswordFindRateLimit(String ip) {
        check("rate:pw-find:" + ip, 5, Duration.ofMinutes(10));
    }

    private void check(String key, int maxAttempts, Duration window) {
        Long count = redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                List.of(key),
                String.valueOf(window.getSeconds())
        );
        if (count != null && count > maxAttempts) {
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }
}
