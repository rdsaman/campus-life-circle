package com.dzdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dzdp.dto.Result;
import com.dzdp.dto.UserDTO;
import com.dzdp.entity.Follow;
import com.dzdp.mapper.FollowMapper;
import com.dzdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzdp.service.IUserService;
import com.dzdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 关注实现类
 * @Author israein
 * @date 20:56 2023/6/1
 **/
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;

    /**
     * 取消关注
     * @Author israein
     * @date 20:56 2023/6/1
     * @param id
     * @return com.dzdp.dto.Result
     **/
    @Override
    public Result isFollow(Long id) {
        // 1.获取登录用户
        UserDTO user = UserHolder.getUser();
        // 2.查询是否关注 select count(*) from tb_follow where user_id = ? and follow_user_id = ?
        Integer count = query().eq("user_id", user.getId()).eq("follow_user_id", id).count();
        // 3.返回
        return Result.ok(count > 0);
    }

    /**
     * 关注
     * @Author israein
     * @date 21:01 2023/6/1
     * @param id
     * @param isFollow
     * @return com.dzdp.dto.Result
     **/
    @Override
    public Result follow(Long id, Boolean isFollow) {
        // 1.获取登录用户
        UserDTO user = UserHolder.getUser();
        String key = "follows:" + user.getId();
        // 2.判断本次操作是关注还是取关
        if (isFollow) {
            // 2.1.关注, 新增记录到数据库
            Follow follow = new Follow();
            follow.setFollowUserId(id);
            follow.setUserId(user.getId());
            boolean isSuccess = save(follow);
            if (isSuccess) {
                // 把关注用户的id, 放入redis的set集合 sadd userId followerUserId
                stringRedisTemplate.opsForSet().add(key, id.toString());
            }
        } else {
            // 2.2.取关, 删除数据库中的记录 delete from tb_follow where user_id = ? and follow_user_id = ?
            boolean isSuccess = remove(new QueryWrapper<Follow>()
                    .eq("user_id", user.getId())
                    .eq("follow_user_id", id));
            if (isSuccess) {
                // 把关注的用户的id从redis集合中移除
                stringRedisTemplate.opsForSet().remove(key, id.toString());
            }
        }
        // 3.返回
        return Result.ok();
    }

    @Override
    public Result followCommons(Long id) {
        // 1.获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;
        // 2.求交集
        String key2 = "follows:" + id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
        if (intersect == null || intersect.isEmpty()) {
            // 没有交集
            return Result.ok(Collections.emptyList());
        }
        // 有交集
        // 3.解析id集合
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        // 4.查询用户
        List<UserDTO> users = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(users);
    }
}
