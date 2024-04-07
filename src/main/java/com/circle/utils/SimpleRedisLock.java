package com.circle.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
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
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>(); // 实例化对象
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua")); // 设置脚本文件位置
        UNLOCK_SCRIPT.setResultType(Long.class); // 设置返回值类型
    }


    /**
     * 获取锁
     * @Author israein
     * @date 21:07 2023/5/15
     * @param timeoutSec
     * @return boolean
     **/
    @Override
    public boolean tryLock(long timeoutSec) {
        // 1.获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 2.获取锁
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
        // 通过del删除锁 (有误删问题)
        // stringRedisTemplate.delete(KEY_PREFIX + name);

        // 1.获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 2.获取锁中的标识
        // String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        // 3.判断标识是否一致
        // if (threadId.equals(id)) {
            // 4.释放锁
            // stringRedisTemplate.delete(KEY_PREFIX + name);
        // }

        // 基于Lua脚本实现分布式锁的释放锁逻辑
        stringRedisTemplate.execute(UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                threadId);
    }
}
