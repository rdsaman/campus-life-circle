package com.dzdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dzdp.dto.Result;
import com.dzdp.dto.UserDTO;
import com.dzdp.entity.Follow;
import com.dzdp.mapper.FollowMapper;
import com.dzdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzdp.utils.UserHolder;
import org.springframework.stereotype.Service;

/**
 * 关注实现类
 * @Author israein
 * @date 20:56 2023/6/1
 **/
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

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
        // 2.判断本次操作是关注还是取关
        if (isFollow) {
            // 2.1.关注, 新增记录到数据库
            Follow follow = new Follow();
            follow.setFollowUserId(id);
            follow.setUserId(user.getId());
            boolean isSuccess = save(follow);
        } else {
            // 2.2.取关, 删除数据库中的记录
            remove(new QueryWrapper<Follow>()
                    .eq("user_id", user.getId())
                    .eq("follow_user_id", id));
        }
        // 3.返回
        return Result.ok();
    }
}
