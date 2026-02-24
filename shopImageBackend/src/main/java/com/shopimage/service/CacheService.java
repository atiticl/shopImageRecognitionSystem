package com.shopimage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CacheService {
    private final StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX = "img:md5:";

    public void putResult(String md5, String resultJson, Duration ttl) {
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + md5, resultJson, ttl);
    }

    public String getResult(String md5) {
        return stringRedisTemplate.opsForValue().get(KEY_PREFIX + md5);
    }
}