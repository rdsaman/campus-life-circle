package com.dzdp;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import com.dzdp.dto.UserDTO;
import com.dzdp.entity.Shop;
import com.dzdp.entity.User;
import com.dzdp.service.IUserService;
import com.dzdp.service.impl.ShopServiceImpl;
import com.dzdp.utils.CacheClient;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.dzdp.utils.RedisConstants.*;


@SpringBootTest
class DzDianPingApplicationTests {
    @Resource
    private CacheClient cacheClient;
    @Resource
    private ShopServiceImpl shopService;
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
}
