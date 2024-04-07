package com.circle.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.circle.dto.LoginFormDTO;
import com.circle.dto.Result;
import com.circle.entity.User;

import javax.servlet.http.HttpSession;

/**
 * 用户接口
 * @Author israein
 * @date 15:07 2023/6/8
 **/
public interface IUserService extends IService<User> {
    /**
     * 发送手机验证码
     * @Author israein
     * @date 21:05 2023/5/6
     * @param phone
     * @param session
     * @return com.dzdp.dto.Result
     **/
    Result sendCode(String phone, HttpSession session);

    /**
     * 实现登录功能
     * @Author israein
     * @date 21:29 2023/5/6
     * @param loginForm
     * @param session
     * @return com.dzdp.dto.Result
     **/
    Result login(LoginFormDTO loginForm, HttpSession session);

    /**
     * 签到功能
     * @Author israein
     * @date 15:51 2023/6/6
     * @return com.dzdp.dto.Result
     **/
    Result sign();

    /**
     * 签到统计
     * @Author israein
     * @date 15:07 2023/6/8
     * @return com.dzdp.dto.Result
     **/
    Result signCount();
}
