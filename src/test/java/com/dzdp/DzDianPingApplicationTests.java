package com.dzdp;

import com.dzdp.entity.Shop;
import com.dzdp.service.impl.ShopServiceImpl;
import com.dzdp.utils.CacheClient;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.dzdp.utils.RedisConstants.CACHE_SHOP_KEY;


@SpringBootTest
class DzDianPingApplicationTests {
    @Resource
    private CacheClient cacheClient;
    @Resource
    private ShopServiceImpl shopService;
    @Resource
    private RedissonClient redissonClient;

    @Test
    public void testSaveShop() {
        Shop shop = shopService.getById(1L);
        cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY + 1L, shop, 10L, TimeUnit.SECONDS);
    }

    @Test
    public void testRedisson() throws InterruptedException {
        RLock lock = redissonClient.getLock("anyLock");
        boolean isLock = lock.tryLock(1, 10, TimeUnit.SECONDS);
        if (isLock) {
            try {
                System.out.println("获取锁成功, 可以开始执行业务");
            } finally {
                lock.unlock();
            }
        }
    }
}
