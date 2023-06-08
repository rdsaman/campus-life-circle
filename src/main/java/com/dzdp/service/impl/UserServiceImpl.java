package com.dzdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzdp.dto.LoginFormDTO;
import com.dzdp.dto.Result;
import com.dzdp.dto.UserDTO;
import com.dzdp.entity.User;
import com.dzdp.mapper.UserMapper;
import com.dzdp.service.IUserService;
import com.dzdp.utils.RegexUtils;
import com.dzdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.TimeoutUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.dzdp.utils.RedisConstants.*;
import static com.dzdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * 服务实现类
 * @Author israein
 * @date 16:34 2023/5/7
 **/
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 发送手机验证码
     * @Author israein
     * @date 21:08 2023/5/6
     * @param phone
     * @param session
     * @return com.dzdp.dto.Result
     **/
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1.接收验证码前校验手机号是否合法
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 如果不符合, 返回错误信息
            return Result.fail("手机号格式错误!");
        }
        // 2.符合, 生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 3.保存验证码
        // session.setAttribute("code", code);
        // 优化, 保存到redis中
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        // 4.发送验证码, 暂时在控制台模拟发送, 之后引入邮箱发送
        log.debug("发送短信验证码成功, 验证码: {}", code);
        // 5.返回ok
        return Result.ok();
    }

    /**
     * 实现登录功能
     * @Author israein
     * @date 21:28 2023/5/6
     * @param loginForm
     * @param session
     * @return com.dzdp.dto.Result
     **/
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.获取手机号和验证码
        String phone = loginForm.getPhone();
        // 2.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误!");
        }
        String code = loginForm.getCode();
        // 3.校验验证码 要考虑到未发送验证码的情况 对于String类型要用equals方法
        // Object cacheCode = session.getAttribute("code");
        // 从redis中获取验证码并校验
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);

        if (cacheCode == null || !cacheCode.equals(code)) {
            // 不一致, 返回错误信息
            return Result.fail("验证码不一致!");
        }
        // 一致
        // 4.根据手机号查询用户
        User user = query().eq("phone", phone).one();
        // 5.判断用户是否存在
        if (user == null) {
            // 用户不存在, 则创建新用户, 并保存到数据库
            user = createUserWithPhone(phone);
        }
        // 用户存在
        // 6.保存用户到session
        // session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class)); //工具类自动将uer封装到UserDTO

        // 保存用户信息到redis
        // 6.1.随机生成token, 作为登录令牌
        String token = UUID.randomUUID().toString(true);
        // 6.2.将User对象转为HashMap存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        // 6.3.存储
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        // 6.4.设置token有效期
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 7.返回token
        return Result.ok(token);
    }

    /**
     * 签到功能
     * @Author israein
     * @date 15:52 2023/6/6
     * @return com.dzdp.dto.Result
     **/
    @Override
    public Result sign() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.写入Redis SETBIT key offset 1
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

    /**
     * 签到统计(最长连续签到天数)
     * @Author israein
     * @date 15:07 2023/6/8
     * @return com.dzdp.dto.Result
     **/
    @Override
    public Result signCount() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.从redis中读取数据: 获取本月到今天的所有签到记录, 返回一个十进制数字 BITFIELD key GET u14 0
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if (result == null || result.isEmpty()) {
            // 6.结果为空表示没有签到记录
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return Result.ok(0);
        }
        // 7.循环遍历
        int count = 0;
        int maxCount = 0;
        while (num != 0) {
            // 7.1.让这个数字与1做与运算，得到数字的最后一个bit位  // 判断这个bit位是否为0
            if ((num & 1) != 0) {
                count++;
            } else {
                if (count > maxCount) {
                    maxCount = count;
                }
                count = 0;
            }
            num >>>= 1; // 右移一位
        }
        return Result.ok(Math.max(count, maxCount));
    }

    /**
     * 根据手机号创建用户
     * @Author israein
     * @date 21:29 2023/5/6
     * @param phone
     * @return com.dzdp.entity.User
     **/
    private User createUserWithPhone(String phone) {
        // 1.创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        // 2.保存用户
        save(user);
        return user;
    }


}
