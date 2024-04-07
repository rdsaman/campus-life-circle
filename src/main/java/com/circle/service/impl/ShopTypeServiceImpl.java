package com.circle.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.circle.dto.Result;
import com.circle.entity.ShopType;
import com.circle.mapper.ShopTypeMapper;
import com.circle.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 商铺类型服务实现类
 * @Author israein
 * @date 14:54 2023/5/8
 **/
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询商铺类型
     * @Author israein
     * @date 15:11 2023/5/8
     * @return com.dzdp.dto.Result
     **/
    @Override
    public Result queryShopType() {
        // 1.获取商铺类型key
        String key = "cache:shop-type-list";
        String cacheShopTypeList = stringRedisTemplate.opsForValue().get(key);
        // 2.查询redis中是否存在商铺类型缓存
        // 3.判断是否存在
        if (StrUtil.isNotBlank(cacheShopTypeList)) {
            // 存在
            List<ShopType> shopTypeList = JSONUtil.toList(cacheShopTypeList, ShopType.class);
            // 直接返回
            return Result.ok(shopTypeList);
        }
        // 不存在
        // 4.查询数据库是否存在
        List<ShopType> shopTypeList = query().orderByAsc("sort").list();
        if (shopTypeList == null) {
            return Result.fail("商铺类型不存在!");
        }
        // 存在
        // 5.将数据写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shopTypeList));
        // 6.返回
        return Result.ok(shopTypeList);
    }
}
