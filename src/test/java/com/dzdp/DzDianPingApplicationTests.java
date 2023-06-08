package com.dzdp;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import com.dzdp.dto.UserDTO;
import com.dzdp.entity.Shop;
import com.dzdp.entity.User;
import com.dzdp.service.IShopService;
import com.dzdp.service.IUserService;
import com.dzdp.service.impl.ShopServiceImpl;
import com.dzdp.utils.CacheClient;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.dzdp.utils.RedisConstants.*;


@SpringBootTest
class DzDianPingApplicationTests {
    @Resource
    private CacheClient cacheClient;
    @Resource
    private IShopService shopService;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;

    // @Test
    // public void testSaveShop() {
    //     Shop shop = shopService.getById(1L);
    //     cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY + 1L, shop, 10L, TimeUnit.SECONDS);
    // }

    // @Test
    // public void testRedisson() throws InterruptedException {
    //     RLock lock = redissonClient.getLock("anyLock");
    //     boolean isLock = lock.tryLock(1, 10, TimeUnit.SECONDS);
    //     if (isLock) {
    //         try {
    //             System.out.println("获取锁成功, 可以开始执行业务");
    //         } finally {
    //             lock.unlock();
    //         }
    //     }
    // }

    /**
     * 生成token
     * @Author israein
     * @date 17:04 2023/5/21
     **/
    // @Test
    // public void testSetToken() {
    //     List<User> list = userService.list();
    //     for (User user : list){
    //         String token = UUID.randomUUID().toString(true);
    //         // 6.2.将User对象转为HashMap存储
    //         UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
    //         Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
    //                 CopyOptions.create()
    //                         .setIgnoreNullValue(true)
    //                         .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
    //         // 6.3.存储
    //         String tokenKey = LOGIN_USER_KEY + token;
    //         stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
    //         // 6.4.设置token有效期
    //         stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
    //     }
    //
    // }

    // @Test
    // public void testGet() {
    //     Set<String> keys = stringRedisTemplate.keys("login:token:*");
    //     int count = 200;
    //     for (String s : keys) {
    //         System.out.println(s.substring(12));
    //         count--;
    //         if (count == 0) {
    //             break;
    //         }
    //     }
    // }

    // @Test
    // public void testOutputToken() throws Exception {
    //     FileWriter fw = new FileWriter("F:\\token.txt");
    //     Set<String> keys = stringRedisTemplate.keys("login:token:");
    //     for (String s : keys) {
    //         fw.write(s);
    //     }
    //     fw.close();
    // }

    // @Test
    // public void testCreateStreamAndGroup() {
    //     stringRedisTemplate.opsForStream().createGroup(
    //
    //     )
    // }

    /**
     * 导入店铺数据到GEO
     * @Author israein
     * @date 15:54 2023/6/5
     **/
    // @Test
    // void testLoadShopData() {
    //     // 1.查询店铺信息
    //     List<Shop> list = shopService.list();
    //     // 2.把店铺分组, 按照type_id(数据库中字段对应typeId)分组
    //     Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
    //     // 3.分批写入redis
    //     for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
    //         // 3.1.获取类型id
    //         Long typeId = entry.getKey();
    //         String key = SHOP_GEO_KEY + typeId;
    //         // 3.2.获取同类型的店铺集合
    //         List<Shop> value = entry.getValue();
    //         List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
    //         // 3.3.写入redis GEOADD key 经度 纬度 值
    //         for (Shop shop : value) {
    //             // stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
    //             locations.add(new RedisGeoCommands.GeoLocation<>(
    //                     shop.getId().toString(),
    //                     new Point(shop.getX(), shop.getY())
    //             ));
    //         }
    //         stringRedisTemplate.opsForGeo().add(key, locations);
    //     }
    // }

    @Test
    void testBitMap() {
        int num = 59;
        // String s = Integer.toBinaryString(num);
        // System.out.println(s);
        int count = 0;
        int maxCount = 0;
        while (num != 0) {
            if ((num & 1) != 0) { // & 1 得该位置本身
                count++;
            } else {
                if (count > maxCount) {
                    maxCount = count;
                }
                count = 0;
            }
            num >>>= 1; // 右移一位
        }
        System.out.println(Math.max(count, maxCount));
    }

    @Test
    void testHyperLogLog() {
        String[] users = new String[1000];
        int index = 0;
        for (int i = 1; i <= 1000000; i++) {
            users[index++] = "user_" + i;
            if (i % 1000 == 0) {
                index = 0;
                stringRedisTemplate.opsForHyperLogLog().add("uv", users);
            }
        }
        Long size = stringRedisTemplate.opsForHyperLogLog().size("uv");
        System.out.println(size);
    }
}
