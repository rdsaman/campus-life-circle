package com.circle.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.circle.dto.Result;
import com.circle.entity.Voucher;
import com.circle.mapper.VoucherMapper;
import com.circle.entity.SeckillVoucher;
import com.circle.service.ISeckillVoucherService;
import com.circle.service.IVoucherService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static com.circle.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * 优惠卷服务实现类
 * @Author israein
 * @date 17:07 2023/5/13
 **/
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    /**
     * 添加秒杀卷
     * @Author israein
     * @date 17:09 2023/5/13
     * @param voucher
     **/
    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher(); // 关联的秒杀卷表
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);

        // 保存秒杀库存到redis
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());
    }

}
