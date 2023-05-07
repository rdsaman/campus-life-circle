package com.dzdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dzdp.dto.LoginFormDTO;
import com.dzdp.dto.Result;
import com.dzdp.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
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
}
