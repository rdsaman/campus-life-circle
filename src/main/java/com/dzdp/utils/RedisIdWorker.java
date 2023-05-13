package com.dzdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @Classname RedisIdWorker
 * @Description Redis全局唯一Id 符号位 + 时间戳 + 序列号
 * @Author israein
 * @Date 2023-05-13 16:20
 */

@Component
public class RedisIdWorker {
    // 开始时间戳
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    // 序列号的位数, 封装成常量 方便后期有变化时修改
    private static final int COUNT_BITS = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 获取自定义自增Id
     * @Author israein
     * @date 16:48 2023/5/13
     * @param keyPrefix
     * @return long
     **/
    public long nextId(String keyPrefix) {
        // 1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        // 将时间转为秒级别的 即时间戳
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        // 2.生成序列号
        // 2.1.获取当前日期, 精确到天 避免超过上限(redis自增长上限为2的64次方) 同时以冒号分隔开可以方便统计
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 2.2.自增长
        Long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
        // 3.拼接并返回 将时间戳左移COUNT_BITS(32)位, 再进行或运算, 此时末32位都为0, 或运算末32位即为序列号count
        return timestamp << COUNT_BITS | count;
    }
}
