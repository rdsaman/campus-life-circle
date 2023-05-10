package com.dzdp;

import com.dzdp.entity.Shop;
import com.dzdp.service.impl.ShopServiceImpl;
import com.dzdp.utils.CacheClient;
import org.junit.jupiter.api.Test;
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

    @Test
    public void testSaveShop() {
        Shop shop = shopService.getById(1L);
        cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY + 1L, shop, 10L, TimeUnit.SECONDS);
    }
}
