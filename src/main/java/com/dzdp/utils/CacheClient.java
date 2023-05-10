package com.dzdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dzdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.dzdp.utils.RedisConstants.*;
import static com.dzdp.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * @Classname CacheClient
 * @Description TODO
 * @Author israein
 * @Date 2023-05-09 17:23
 */
@Slf4j
@Component
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 将任意Java对象序列化为json并存储在string类型的key中，并且可以设置TTL过期时间
     * @Author israein
     * @date 22:40 2023/5/10
     * @param key
     * @param value
     * @param time
     * @param unit
     **/
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 将任意Java对象序列化为json并存储在string类型的key中，并且可以设置逻辑过期时间，用于处理缓存击穿问题
     * @Author israein
     * @date 22:43 2023/5/10
     * @param key
     * @param value
     * @param time
     * @param unit
     **/
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        // 设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }


    /**
     * 根据指定的key查询缓存, 使用互斥锁解决缓存击穿问题
     * @Author israein
     * @date 22:57 2023/5/10
     * @param keyPrefix
     * @param id
     * @param type
     * @param dbFallback
     * @param time
     * @param unit
     * @return R
     **/
    public <R, ID> R queryWithMutex(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit
    ) {
        // 1.拿到key
        String key = keyPrefix + id;
        // 2.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 3.判断缓存是否命中
        if (StrUtil.isNotBlank(shopJson)) {
            // 3.1命中
            // 不为空直接返回
            return JSONUtil.toBean(shopJson, type);
        }
        // 3.2命中
        // 3.3判断是否为空值
        if (shopJson != null) {
            return null;
        }
        // 3.4.缓存未命中
        // 4.重建缓存
        // 4.1.获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        R r = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 4.2.判断是否获取锁成功
            if (!isLock) {
                // 4.3.获取锁失败, 休眠并重试
                Thread.sleep(50);
                return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit); // 使用递归是否合适? 有没有其他更好的方案?
            }
            // 4.4.获取锁成功
            // 二次检验是否获取锁成功
            // 根据id查询数据库
            r = dbFallback.apply(id);
            // 模拟重建延时
            Thread.sleep(200);
            // 5.数据库不存在
            if (r == null) {
                // 5.1.数据库不存在, 将空值写入redis
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                // 5.2.返回错误信息
                return null;
            }
            // 6.数据库存在, 写入redis
            this.set(key, r, time, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 7.释放锁
            unLock(lockKey);
        }
        // 8.返回
        return r;
    }

    /**
     * 根据指定的key查询缓存, 并反序列化为指定类型, 需要利用逻辑过期解决缓存击穿问题
     * @Author israein
     * @date 23:12 2023/5/10
     * @param keyPrefix
     * @param id
     * @param type
     * @param dbFallback
     * @param time
     * @param unit
     * @return R
     **/
    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1.从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isBlank(json)) {
            // 3.存在，直接返回
            return null;
        }
        // 4.命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 5.判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())) {
            // 5.1.未过期，直接返回店铺信息
            return r;
        }
        // 5.2.已过期，需要缓存重建
        // 6.缓存重建
        // 6.1.获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 6.2.判断是否获取锁成功
        if (isLock){
            // 6.3.成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库
                    R newR = dbFallback.apply(id);
                    // 重建缓存
                    this.setWithLogicalExpire(key, newR, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    // 释放锁
                    unLock(lockKey);
                }
            });
        }
        // 6.4.返回过期的商铺信息
        return r;
    }

    /**
     * 根据指定的key查询缓存，利用缓存空值的方式解决缓存穿透问题
     * @Author israein
     * @date 23:12 2023/5/10
     * @param keyPrefix
     * @param id
     * @param type
     * @param dbFallback
     * @param time
     * @param unit
     * @return R
     **/
    public <R, ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        // 1.拿到key
        String key = keyPrefix + id;
        // 2.从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 3.判断缓存是否命中
        if (StrUtil.isNotBlank(json)) {
            // 3.1.命中
            // 不为空直接返回
            return JSONUtil.toBean(json, type);
        }
        // 3.2.命中
        // 判断是否为空值
        if (json != null) {
            return null;
        }
        // 4.缓存未命中
        // 4.1.根据id查询数据库
        R r = dbFallback.apply(id);
        if (r == null) {
            // 4.2.数据库不存在, 将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 4.3.返回错误信息
            return null;
        }
        // 5.存在，写入redis
        this.set(key, r, time, unit);
        // 6.返回
        return r;
    }

    /**
     * 获取锁
     * @Author israein
     * @date 23:43 2023/5/10
     * @param key
     * @return boolean
     **/
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     * @Author israein
     * @date 23:43 2023/5/10
     * @param key
     **/
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }
}
