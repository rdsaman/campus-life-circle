package com.dzdp.service;

import com.dzdp.dto.Result;
import com.dzdp.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 关注接口
 * @Author israein
 * @date 20:55 2023/6/1
 **/
public interface IFollowService extends IService<Follow> {
    /**
     * 取消关注
     * @Author israein
     * @date 20:55 2023/6/1
     * @param id
     * @return com.dzdp.dto.Result
     **/
    Result isFollow(Long id);

    /**
     * 关注
     * @Author israein
     * @date 20:59 2023/6/1
     * @param id
     * @param isFollow
     * @return com.dzdp.dto.Result
     **/
    Result follow(Long id, Boolean isFollow);
}
