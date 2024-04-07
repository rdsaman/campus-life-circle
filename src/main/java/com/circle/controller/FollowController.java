package com.circle.controller;


import com.circle.dto.Result;
import com.circle.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 关注控制器
 * @Author israein
 * @date 20:51 2023/6/1
 **/
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private IFollowService followService;

    /**
     * 取消关注
     * @Author israein
     * @date 21:00 2023/6/1
     * @param id
     * @return com.dzdp.dto.Result
     **/
    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long id) {
        return followService.isFollow(id);
    }

    /**
     * 关注
     * @Author israein
     * @date 21:01 2023/6/1
     * @param id
     * @param isFollow
     * @return com.dzdp.dto.Result
     **/
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long id, @PathVariable("isFollow") Boolean isFollow) {
        return followService.follow(id, isFollow);
    }

    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable("id") Long id) {
        return followService.followCommons(id);
    }
}
