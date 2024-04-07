package com.circle.service;

import com.circle.dto.Result;
import com.circle.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 优惠卷订单服务接口
 * @Author israein
 * @date 18:31 2023/5/13
 **/
public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 秒杀下单
     * @Author israein
     * @date 18:08 2023/5/13
     * @param voucherId
     * @return com.dzdp.dto.Result
     **/
    Result seckillVoucher(Long voucherId);

    void createVoucherOrder(VoucherOrder voucherOrder);
}
