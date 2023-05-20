package com.dzdp.config;


import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Classname RedissonConfig
 * @Description Redisson客户端
 * @Author israein
 * @Date 2023-05-20 16:05
 */
@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() {
        // 配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://47.113.227.164:6379")
                .setPassword("root123");
        // 创建RedissonClient对象
        return Redisson.create(config);
    }
}
