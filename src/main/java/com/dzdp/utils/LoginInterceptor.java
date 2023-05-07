package com.dzdp.utils;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Classname LoginInterceptor
 * @Description 登录拦截器
 * @Author israein
 * @Date 2023-05-06 22:14
 */
public class LoginInterceptor implements HandlerInterceptor {

    /**
     * 登录拦截器, 在处理之前拦截
     * @Author israein
     * @date 22:15 2023/5/6
     * @param request
     * @param response
     * @param handler
     * @return boolean
     **/
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.判断是否需要拦截 (ThreadLocal中是否有用户)
        if (UserHolder.getUser() == null) {
            // 没有用户, 需要拦截, 设置状态码
            response.setStatus(401);
            // 拦截
            return false;
        }
        // 2.有用户, 放行
        return true;
    }
}
