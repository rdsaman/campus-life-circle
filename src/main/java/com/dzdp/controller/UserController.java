package com.dzdp.controller;


import cn.hutool.core.util.RandomUtil;
import com.dzdp.dto.LoginFormDTO;
import com.dzdp.dto.Result;
import com.dzdp.dto.UserDTO;
import com.dzdp.entity.User;
import com.dzdp.entity.UserInfo;
import com.dzdp.service.IUserInfoService;
import com.dzdp.service.IUserService;
import com.dzdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * 控制器
 * @Author israein
 * @date 20:27 2023/5/6
 **/
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     * @Author israein
     * @date 20:43 2023/5/6
     * @param phone
     * @param session
     * @return com.dzdp.dto.Result
     **/
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone, session);
    }

    /**
     * 登录功能
     * @Author israein
     * @date 22:33 2023/5/6
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     * @param session
     * @return com.dzdp.dto.Result
     **/
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        return userService.login(loginForm, session);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(){
        // TODO 实现登出功能
        return Result.fail("功能未完成");
    }

    /**
     * 获取当前登录的用户并返回
     * @Author israein
     * @date 22:34 2023/5/6
     * @return com.dzdp.dto.Result
     **/
    @GetMapping("/me")
    public Result me(){
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }
}
