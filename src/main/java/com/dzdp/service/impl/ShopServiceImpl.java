package com.dzdp.service.impl;

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

import static com.dzdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.dzdp.utils.RedisConstants.CACHE_SHOP_TTL;

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
        // 1.拿到key
        String key = CACHE_SHOP_KEY + id;
        // 2.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 3.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 存在
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        // 不存在
        // 4.根据id查询数据库
        Shop shop = getById(id);
        if (shop == null) {
            return Result.fail("商铺不存在!");
        }
        // 5.存在, 写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 6.返回
        return Result.ok(shop);
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
