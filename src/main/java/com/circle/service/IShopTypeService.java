package com.circle.service;

import com.circle.dto.Result;
import com.circle.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 商铺类型服务接口
 * @Author israein
 * @date 14:53 2023/5/8
 **/
public interface IShopTypeService extends IService<ShopType> {

    /**
     * 查询商铺类型
     * @Author israein
     * @date 14:53 2023/5/8
     * @return com.dzdp.dto.Result
     **/
    Result queryShopType();
}
