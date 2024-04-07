package com.circle.controller;


import com.circle.dto.Result;
import com.circle.service.IVoucherOrderService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 秒杀下单
 * @Author israein
 * @date 17:49 2023/5/13
 **/
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    @Resource
    private IVoucherOrderService voucherOrderService;

    /**
     * 秒杀下单
     * @Author israein
     * @date 19:42 2023/5/29
     * @param voucherId
     * @return com.dzdp.dto.Result
     **/
    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }
}
