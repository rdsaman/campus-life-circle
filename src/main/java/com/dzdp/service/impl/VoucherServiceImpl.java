package com.dzdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzdp.dto.Result;
import com.dzdp.entity.Voucher;
import com.dzdp.entity.VoucherOrder;
import com.dzdp.mapper.VoucherMapper;
import com.dzdp.entity.SeckillVoucher;
import com.dzdp.service.ISeckillVoucherService;
import com.dzdp.service.IVoucherService;
import com.dzdp.utils.RedisIdWorker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠卷服务实现类
 * @Author israein
 * @date 17:07 2023/5/13
 **/
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

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
    }

}
