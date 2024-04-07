package com.circle.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.circle.dto.Result;
import com.circle.entity.VoucherOrder;
import com.circle.mapper.VoucherOrderMapper;
import com.circle.service.ISeckillVoucherService;
import com.circle.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.circle.utils.RedisIdWorker;
import com.circle.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 优惠卷订单服务实现类
 * @Author israein
 * @date 18:31 2023/5/13
 **/
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    // private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
    // private IVoucherOrderService proxy;



    // 异步处理线程池
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    //在类初始化之后执行，因为当这个类初始化好了之后，随时都是有可能要执行的
    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }
    // 用于线程池处理的任务
    // 当初始化完毕后，就会去从队列中去拿信息
    private class VoucherOrderHandler implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    // 消息队列如果不存在时会自动创建
                    // 1.获取redis消息队列中的订单信息 XREADGROUP GOUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create("stream.orders", ReadOffset.lastConsumed())
                    );
                    // 2.判断是否为空
                    if (list == null || list.isEmpty()) {
                        // 如果为空则说明没有消息, 重试
                        continue;
                    }
                    // 3.解析数据
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 4.创建订单
                    createVoucherOrder(voucherOrder);
                    // 5.确认消息 XACK
                    stringRedisTemplate.opsForStream().acknowledge("stream.orders", "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    handlePendingList();
                }
            }
        }

        private void handlePendingList() {
            while (true) {
                try {
                    // 1.获取pending-list中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 0
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1), // 获取一条数据
                            StreamOffset.create("stream.orders", ReadOffset.from("0"))
                    );
                    // 2.判断是否为空
                    if (list == null || list.isEmpty()) {
                        // 如果为空说明没有异常
                        break;
                    }
                    // 3.解析数据
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 4.创建订单
                    createVoucherOrder(voucherOrder);
                    // 5.确认消息 XACK
                    stringRedisTemplate.opsForStream().acknowledge("stream.orders", "g1", record.getId());
                } catch (Exception e) {
                    log.error("订单处理异常!", e);
                }
            }
        }
    }

    // private class VoucherOrderHandler implements Runnable {
    //
    //     @Override
    //     public void run() {
    //         while (true) {
    //             try {
    //                 // 1.获取队列中的订单信息
    //                 VoucherOrder voucherOrder = orderTasks.take();
    //                 // 2.创建订单
    //                 handleVoucherOrder(voucherOrder);
    //             } catch (Exception e) {
    //                 log.error("处理订单异常", e);
    //             }
    //         }
    //     }
    //
    //     private void handleVoucherOrder(VoucherOrder voucherOrder) {
    //         //1.获取用户
    //         Long userId = voucherOrder.getUserId();
    //         // 2.创建锁对象
    //         RLock redisLock = redissonClient.getLock("lock:order:" + userId);
    //         // 3.尝试获取锁
    //         boolean isLock = redisLock.tryLock();
    //         // 4.判断是否获得锁成功
    //         if (!isLock) {
    //             // 获取锁失败，直接返回失败或者重试
    //             log.error("不允许重复下单！");
    //             return;
    //         }
    //         try {
    //             //注意：由于是spring的事务是放在threadLocal中，此时的是多线程，事务会失效
    //             proxy.createVoucherOrder(voucherOrder);
    //         } finally {
    //             // 释放锁
    //             redisLock.unlock();
    //         }
    //     }
    // }

    /**
     * 秒杀下单
     * @Author israein
     * @date 20:33 2023/5/29
     * @param voucherId
     * @return com.dzdp.dto.Result
     **/
    public Result seckillVoucher(Long voucherId) {
        // 获取用户
        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdWorker.nextId("order");
        // 1.执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );
        int r = result.intValue();
        // 2.判断结果是否为0
        if (r != 0) {
            // 2.1.不为0说明没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }

        // // 使用阻塞队列
        // // 3.创建订单
        // VoucherOrder voucherOrder = new VoucherOrder();
        // // 3.1.订单id
        // // orderId = redisIdWorker.nextId("order");
        // voucherOrder.setId(orderId);
        // // 3.2.用户id
        // voucherOrder.setUserId(userId);
        // // 3.3.代金卷id
        // voucherOrder.setVoucherId(voucherId);
        // // 3.4.放入阻塞队列
        // orderTasks.add(voucherOrder);
        // // 4.获取代理类对象
        // proxy = (IVoucherOrderService) AopContext.currentProxy();


        // 5.返回订单id
        return Result.ok(orderId);
    }

    /**
     * 创建订单(使用Redisson锁, 二次判断是否重复下单或库存不足)
     * @Author israein
     * @date 21:02 2023/5/30
     * @param voucherOrder
     **/
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();
        // 创建锁对象
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 尝试获取锁
        boolean isLock = lock.tryLock();
        if (!isLock) {
            // 获取锁失败
            log.error("不允许重复下单!");
            return;
        }
        try {
            // 5.1.查询订单
            int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
            // 5.2.判断是否存在
            if (count > 0) {
                // 5.3.存在, 用户已经购买过了
                log.error("不允许重复下单!");
                return;
            }

            // 6.扣减库存
            boolean success = seckillVoucherService.update()
                    .setSql("stock = stock - 1")
                    .eq("voucher_id", voucherId)
                    .gt("stock",0)
                    .update();
            if (!success) {
                // 扣减失败
                log.error("库存不足!");
            }
            // 7.创建订单
            save(voucherOrder);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    /**
     * 创建订单(使用事务)
     * @Author israein
     * @date 20:16 2023/5/29
     * @param voucherOrder
     **/
    // @Transactional
    // public void createVoucherOrder(VoucherOrder voucherOrder) {
    //     // 5.添加一人一单逻辑
    //     // 5.1.获取用户id !!! 这里是放在线程池执行方法中的, 所以不能通过ThreadLocal获取
    //     Long userId = voucherOrder.getUserId();
    //     // 5.2.获取订单表中的记录值
    //     int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
    //     // 5.3.判断是否存在
    //     if (count > 0) {
    //         // 5.4.用户已经购买过了
    //         log.error("用户已经购买过一次了!");
    //         // return Result.fail("用户已经购买过一次了!");
    //     }
    //
    //     // 6.扣减库存
    //     boolean success = seckillVoucherService.update()
    //             .setSql("stock = stock - 1")
    //             .eq("voucher_id", voucherOrder.getVoucherId())
    //             .gt("stock",0)
    //             .update();
    //     if (!success) {
    //         log.error("库存不足!");
    //         // return Result.fail("库存不足!");
    //     }
    //     // // 7.创建订单
    //     // VoucherOrder voucherOrder = new VoucherOrder();
    //     // // 7.1.订单id
    //     // long orderId = redisIdWorker.nextId("order");
    //     // voucherOrder.setId(orderId);
    //     // // 7.2.用户id
    //     // userId = UserHolder.getUser().getId();
    //     // voucherOrder.setUserId(userId);
    //     // // 7.3.代金卷id
    //     // voucherOrder.setVoucherId(voucherId);
    //     // 7.4.保存
    //     save(voucherOrder);
    //     // 8.返回订单id
    //     // return Result.ok(orderId);
    // }

    // @Override
    // public Result seckillVoucher(Long voucherId) {
    //     // 1.查询优惠卷
    //     SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
    //     // 2.判断秒杀是否开始
    //     if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
    //         // 2.1.秒杀尚未开始
    //         return Result.fail("秒杀尚未开始!");
    //     }
    //     // 3.判断秒杀是否已经结束
    //     if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
    //         // 3.1.秒杀已经结束
    //         return Result.fail("秒杀已经结束!");
    //     }
    //     // 4.判断库存是否充足
    //     if (voucher.getStock() < 1) {
    //         // 4.1.库存不足
    //         return Result.fail("库存不足!");
    //     }
    //
    //     // 5.添加一人一单逻辑
    //     // 5.1.获取用户id
    //     Long userId = UserHolder.getUser().getId();
    //     // 5.2.获取订单表中的记录值
    //     int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
    //     // 5.3.判断是否存在
    //     if (count > 0) {
    //         // 5.4.用户已经购买过了
    //         return Result.fail("用户已经购买过一次了!");
    //     }
    //
    //     // 6.扣减库存
    //     boolean success = seckillVoucherService.update()
    //             .setSql("stock = stock - 1")
    //             .eq("voucher_id", voucherId)
    //             .gt("stock",0)
    //             .update();
    //     if (!success) {
    //         return Result.fail("库存不足!");
    //     }
    //     // 7.创建订单
    //     VoucherOrder voucherOrder = new VoucherOrder();
    //     // 7.1.订单id
    //     long orderId = redisIdWorker.nextId("order");
    //     voucherOrder.setId(orderId);
    //     // 7.2.用户id
    //     userId = UserHolder.getUser().getId();
    //     voucherOrder.setUserId(userId);
    //     // 7.3.代金卷id
    //     voucherOrder.setVoucherId(voucherId);
    //     // 7.4.保存
    //     save(voucherOrder);
    //     // 8.返回订单id
    //     return Result.ok(orderId);
    // }

    // @Override
    // public Result seckillVoucher(Long voucherId) {
    //     // 1.查询优惠卷
    //     SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
    //     // 2.判断秒杀是否开始
    //     if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
    //         // 2.1.秒杀尚未开始
    //         return Result.fail("秒杀尚未开始!");
    //     }
    //     // 3.判断秒杀是否已经结束
    //     if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
    //         // 3.1.秒杀已经结束
    //         return Result.fail("秒杀已经结束!");
    //     }
    //     // 4.判断库存是否充足
    //     if (voucher.getStock() < 1) {
    //         // 4.1.库存不足
    //         return Result.fail("库存不足!");
    //     }
    //     Long userId = UserHolder.getUser().getId();
    //     // 分布式锁
    //     // 5.创建锁对象
    //     // 使用Redisson分布式锁
    //     // redissonClient.getMultiLock(); // 联锁
    //     RLock lock = redissonClient.getLock("lock:order:" + userId);
    //     // SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
    //     // 5.1.获取锁对象
    //     // boolean isLock = lock.tryLock(1200);
    //     boolean isLock = lock.tryLock();
    //     // 5.2.加锁失败
    //     if (!isLock) {
    //         return Result.fail("不允许重复下单!");
    //     }
    //     try {
    //         // 需要使用代理让事务生效, 获取代理对象(事务)
    //         IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
    //         return proxy.createVoucherOrder(voucherId);
    //     }  finally {
    //         // 释放锁
    //         lock.unlock();
    //     }


        // 单体环境下的一人一单功能
        // synchronized (userId.toString().intern()) {
        //     // 用this 调用的是当前实现类对象, 如果有多次调用, 每次都能够获取到, 事务失效了
        //     // return this.createVoucherOrder(voucherId);
        //     // 因此需要使用代理让事务生效, 获取代理对象(事务)
        //     IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
        //     return proxy.createVoucherOrder(voucherId);
        // }
    // }


}
