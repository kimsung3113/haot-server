package com.haot.coupon.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Integer> countRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Integer> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericToStringSerializer<>(Integer.class));
        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        return redisTemplate;
    }

    @Bean
    public DefaultRedisScript<String> limitedCouponIssueScript() {
        String script = """
                if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
                    return '2'
                end
                
                local count = redis.call('DECR', KEYS[1])
                if tonumber(count) < 0 then
                    redis.call('INCR', KEYS[1])
                    return '3'
                end
                
                redis.call('SADD', KEYS[2], ARGV[1])
                
                if redis.call('SCARD', KEYS[2]) == 1 then
                    redis.call('EXPIRE', KEYS[2], ARGV[2])
                end
                
                return '1'
                """;
        return new DefaultRedisScript<>(script, String.class);
    }

    @Bean
    public DefaultRedisScript<String> unlimitedCouponIssueScript() {
        String script = """
                if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then
                    return '2'
                end

                redis.call('SADD', KEYS[1], ARGV[1])
                
                if redis.call('SCARD', KEYS[1]) == 1 then
                    redis.call('EXPIRE', KEYS[1], ARGV[2])
                end
                
                return '1'
                """;
        return new DefaultRedisScript<>(script, String.class);
    }


}
