
package com.event_registration_platform.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PasswordResetService {

    private final RedisTemplate<String, String> redisTemplate;

    public PasswordResetService(
            RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateResetToken(String email) {

        String token = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(
                token,
                email,
                15,
                TimeUnit.MINUTES
        );

        return token;
    }

    public String getEmailByToken(String token) {
        return redisTemplate.opsForValue().get(token);
    }

    public void deleteToken(String token) {
        redisTemplate.delete(token);
    }
}
