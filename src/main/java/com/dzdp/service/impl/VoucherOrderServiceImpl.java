package com.dzdp.service.impl;

import com.dzdp.dto.Result;
import com.dzdp.entity.SeckillVoucher;
import com.dzdp.entity.VoucherOrder;
import com.dzdp.mapper.VoucherOrderMapper;
import com.dzdp.service.ISeckillVoucherService;
import com.dzdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzdp.utils.RedisIdWorker;
import com.dzdp.utils.SimpleRedisLock;
import com.dzdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 优惠卷订单服务实现类
 * @Author israein
 * @date 18:31 2023/5/13
 **/
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisIdWorker redisIdWorker;

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

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1.查询优惠卷
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2.判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // 2.1.秒杀尚未开始
            return Result.fail("秒杀尚未开始!");
        }
        // 3.判断秒杀是否已经结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            // 3.1.秒杀已经结束
            return Result.fail("秒杀已经结束!");
        }
        // 4.判断库存是否充足
        if (voucher.getStock() < 1) {
            // 4.1.库存不足
            return Result.fail("库存不足!");
        }
        Long userId = UserHolder.getUser().getId();
        // 分布式锁
        // 5.创建锁对象
        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        // 5.1.获取锁对象
        boolean isLock = lock.tryLock(1200);
        // 5.2.加锁失败
        if (!isLock) {
            return Result.fail("不允许重复下单!");
        }
        try {
            // 需要使用代理让事务生效, 获取代理对象(事务)
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }  finally {
            // 释放锁
            lock.unLock();
        }


        // 单体环境下的一人一单功能
        // synchronized (userId.toString().intern()) {
        //     // 用this 调用的是当前实现类对象, 如果有多次调用, 每次都能够获取到, 事务失效了
        //     // return this.createVoucherOrder(voucherId);
        //     // 因此需要使用代理让事务生效, 获取代理对象(事务)
        //     IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
        //     return proxy.createVoucherOrder(voucherId);
        // }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 5.添加一人一单逻辑
        // 5.1.获取用户id
        Long userId = UserHolder.getUser().getId();
        // 5.2.获取订单表中的记录值
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 5.3.判断是否存在
        if (count > 0) {
            // 5.4.用户已经购买过了
            return Result.fail("用户已经购买过一次了!");
        }

        // 6.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock",0)
                .update();
        if (!success) {
            return Result.fail("库存不足!");
        }
        // 7.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 7.1.订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 7.2.用户id
        userId = UserHolder.getUser().getId();
        voucherOrder.setUserId(userId);
        // 7.3.代金卷id
        voucherOrder.setVoucherId(voucherId);
        // 7.4.保存
        save(voucherOrder);
        // 8.返回订单id
        return Result.ok(orderId);
    }
}
