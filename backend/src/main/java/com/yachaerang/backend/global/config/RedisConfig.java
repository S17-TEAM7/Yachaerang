package com.yachaerang.backend.global.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

@EnableCaching
@RequiredArgsConstructor
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;
    @Value("${spring.data.redis.port}")
    private int redisPort;
    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    // Redis Lettuce 기반 연결
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setPassword(redisPassword);
        return new LettuceConnectionFactory(config);
    }

    // Cache 전용
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
        ObjectMapper cacheMapper = new ObjectMapper();
        cacheMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        cacheMapper.registerModule(new JavaTimeModule());
        cacheMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(cacheMapper);

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                "product:itemNames",  defaults.entryTtl(Duration.ofHours(24)),
                "product:subItems",   defaults.entryTtl(Duration.ofHours(24)),
                "daily:rank:high",    defaults.entryTtl(Duration.ofHours(24)),
                "daily:rank:low",     defaults.entryTtl(Duration.ofHours(24)),
                "daily:price",        defaults.entryTtl(Duration.ofDays(7)),
                "weekly:price",       defaults.entryTtl(Duration.ofDays(7)),
                "monthly:price",      defaults.entryTtl(Duration.ofDays(30)),
                "yearly:price",       defaults.entryTtl(Duration.ofDays(365))
        );

        return RedisCacheManager.builder(cf)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
