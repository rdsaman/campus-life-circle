package com.circle.controller;


import com.circle.dto.Result;
import com.circle.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 控制器
 * @Author israein
 * @date 20:17 2023/5/6
 **/
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    @GetMapping("list")
    public Result queryTypeList() {
        // List<ShopType> typeList = typeService
        //         .query().orderByAsc("sort").list();
        // return Result.ok(typeList);
        return typeService.queryShopType();
    }
}
