package com.dzdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @Classname SimpleRedisLock
 * @Description 分布式锁对象
 * @Author israein
 * @Date 2023-05-15 16:01
 */
public class SimpleRedisLock implements ILock{
    private String name;
    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    /**
     * 获取锁
     * @Author israein
     * @date 21:07 2023/5/15
     * @param timeoutSec
     * @return boolean
     **/
    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程标示
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放锁
     * @Author israein
     * @date 21:07 2023/5/15
     **/
    @Override
    public void unLock() {
        // 通过del删除锁
        stringRedisTemplate.delete(KEY_PREFIX + name);
    }
}
