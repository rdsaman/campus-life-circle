package com.dzdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.dzdp.dto.Result;
import com.dzdp.entity.Shop;
import com.dzdp.mapper.ShopMapper;
import com.dzdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.dzdp.utils.RedisConstants.*;

/**
 * 商铺服务实现类
 * @Author israein
 * @date 14:37 2023/5/8
 **/
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据id查询商铺
     * @Author israein
     * @date 15:41 2023/5/8
     * @param id
     * @return com.dzdp.dto.Result
     **/
    @Override
    public Result queryById(Long id) {
        // 解决缓存穿透
        Shop shop = queryWithPassThrough(id);

        // 使用互斥锁解决缓存击穿(有bug)
        // Shop shop = queryWithMutex(id);

        if (shop == null) {
            return Result.fail("店铺不存在!");
        }
        return Result.ok(shop);
    }

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }

    private Shop queryWithMutex(Long id) {
        // 1.拿到key
        String key = CACHE_SHOP_KEY + id;
        // 2.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 3.判断缓存是否命中
        if (StrUtil.isNotBlank(shopJson)) {
            // 命中
            // 不为空直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        // 命中
        // 判断是否为空值
        if (shopJson != null) {
            return null;
        }
        // 缓存未命中
        // 4.重建缓存
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 判断是否获取锁成功
            if (!isLock) {
                // 获取锁失败, 休眠并重试
                Thread.sleep(50);
                return queryWithMutex(id); // 使用递归是否合适? 有没有其他更好的方案?
            }
            // 获取锁成功
            // TODO 二次检验是否获取锁成功
            // 5.根据id查询数据库
            shop = getById(id);
            // 模拟重建延时
            Thread.sleep(200);
            if (shop == null) {
                // 数据库不存在, 将空值写入redis
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                // 返回错误信息
                return null;
            } else {
                // 数据库存在, 写入redis
                stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 释放锁
            unLock(lockKey);
        }
        // 6.返回
        return shop;
    }

    private Shop queryWithPassThrough(Long id) {
        // 1.拿到key
        String key = CACHE_SHOP_KEY + id;
        // 2.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 3.判断缓存是否命中
        if (StrUtil.isNotBlank(shopJson)) {
            // 命中
            // 不为空直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        // 命中
        // 判断是否为空值
        if (shopJson != null) {
            return null;
        }
        // 缓存未命中
        // 4.根据id查询数据库
        Shop shop = getById(id);
        if (shop == null) {
            // 数据库不存在, 将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 返回错误信息
            return null;
        } else {
            // 数据库存在, 写入redis
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        }
        // 5.返回
        return shop;
    }

    /**
     * 更新商铺信息
     * @Author israein
     * @date 15:45 2023/5/8
     * @param shop
     * @return com.dzdp.dto.Result
     **/
    @Override
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空!");
        }
        // 1.更新数据库
        updateById(shop);
        // 2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }
}
