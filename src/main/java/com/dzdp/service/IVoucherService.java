package com.dzdp.service;

import com.dzdp.dto.Result;
import com.dzdp.entity.Voucher;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 优惠卷服务接口
 * @Author israein
 * @date 17:07 2023/5/13
 **/
public interface IVoucherService extends IService<Voucher> {

    /**
     * 查询店铺的优惠券列表
     * @Author israein
     * @date 17:09 2023/5/13
     * @param shopId
     * @return com.dzdp.dto.Result
     **/
    Result queryVoucherOfShop(Long shopId);

    /**
     * 添加秒杀卷
     * @Author israein
     * @date 17:08 2023/5/13
     * @param voucher
     **/
    void addSeckillVoucher(Voucher voucher);

}
