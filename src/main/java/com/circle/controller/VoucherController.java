package com.circle.controller;


import com.circle.dto.Result;
import com.circle.entity.Voucher;
import com.circle.service.IVoucherService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 优惠卷控制器
 * @Author israein
 * @date 17:03 2023/5/13
 **/
@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Resource
    private IVoucherService voucherService;

     /**
     * 新增普通券
     * @Author israein
     * @date 17:05 2023/5/13
     * @param voucher 优惠券信息
     * @return com.dzdp.dto.Result
     **/
    @PostMapping
    public Result addVoucher(@RequestBody Voucher voucher) {
        voucherService.save(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * 新增秒杀券
     * @Author israein
     * @date 17:05 2023/5/13
     * @param voucher 优惠券信息，包含秒杀信息
     * @return com.dzdp.dto.Result
     **/
    @PostMapping("seckill")
    public Result addSeckillVoucher(@RequestBody Voucher voucher) {
        voucherService.addSeckillVoucher(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * 查询店铺的优惠券列表
     * @Author israein
     * @date 17:05 2023/5/13
     * @param shopId 店铺id
     * @return com.dzdp.dto.Result 优惠券列表
     **/
    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
       return voucherService.queryVoucherOfShop(shopId);
    }
}
