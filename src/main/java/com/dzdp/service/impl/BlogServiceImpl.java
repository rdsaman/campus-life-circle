package com.dzdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dzdp.dto.Result;
import com.dzdp.dto.UserDTO;
import com.dzdp.entity.Blog;
import com.dzdp.entity.User;
import com.dzdp.mapper.BlogMapper;
import com.dzdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzdp.service.IUserService;
import com.dzdp.utils.SystemConstants;
import com.dzdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dzdp.utils.RedisConstants.BLOG_LIKED_KEY;

/**
 * 探店笔记服务实现类
 * @Author israein
 * @date 19:56 2023/6/1
 **/
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 保存探店笔记
     * @Author israein
     * @date 20:34 2023/5/31
     * @param blog
     * @return com.dzdp.dto.Result
     **/
    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        save(blog);
        // 返回id
        return Result.ok(blog.getId());
    }

    /**
     * 根据id查询探店笔记
     * @Author israein
     * @date 20:53 2023/5/31
     * @param id
     * @return com.dzdp.dto.Result
     **/
    @Override
    public Result queryBlogById(Long id) {
        // 1.根据id查询探店笔记
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("该探店笔记不存在!");
        }
        // 2.查询相关用户
        // 查询用户
        queryBlogUser(blog);
        // 3.查询探店笔记是否被点赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    private void isBlogLiked(Blog blog) {
        // 1.获取当前登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 用户未登录, 无需查询是否点赞
            return;
        }
        Long userId = user.getId();
        // 2.判断当前登录用户是否已经点赞
        String key = BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    /**
     * 点赞
     * @Author israein
     * @date 20:53 2023/5/31
     * @param id
     * @return com.dzdp.dto.Result
     **/
    @Override
    public Result likeBlog(Long id) {
        // 修改点赞数量(会导致无限点赞)
        // update().setSql("liked = liked + 1").eq("id", id).update();

        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        if (UserHolder.getUser() == null) {
            // 用户未登录, 无需查询是否点赞
            // 拦截器拦截了
            return Result.ok("登录后才可以点赞哦!");
        }
        // 2.判断当前用户是否已经点赞
        String key = BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            // 3.如果未点赞, 可以点赞
            // 3.1.该blog点赞数+1
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            // 3.2.保存用户到Redis的Set集合 zadd key value score
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            // 4.如果已点赞, 取消点赞
            // 4.1.该blog点赞数-1
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            // 4.2.将用户从Redis的Set集合中移除
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 查询我的探店笔记
     * @Author israein
     * @date 20:52 2023/5/31
     * @param current
     * @return com.dzdp.dto.Result
     **/
    @Override
    public Result queryMyBlog(Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    /**
     * 查询热门探店笔记
     * @Author israein
     * @date 20:52 2023/5/31
     * @param current
     * @return com.dzdp.dto.Result
     **/
    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog ->{
            queryBlogUser(blog);
        });
        return Result.ok(records);
    }

    /**
     * 查询点赞列表
     * @Author israein
     * @date 20:20 2023/6/1
     * @param id
     * @return com.dzdp.dto.Result
     **/
    @Override
    public Result queryBlogLikes(Long id) {
        // 1.查询top5的点赞用户 zrange key 0 4
        String key = BLOG_LIKED_KEY + id;
        // 如果在redis中没有查到数据会sql报错
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        // 2.解析出其中的用户id
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        if (ids == null || ids.isEmpty()) {
            return Result.ok("暂时没有人点赞");
        }
        String idStr = StrUtil.join(",", ids);
        // 3.根据id查询用户(5个,封装到List中) where id in (5, 1)  order by field(id, 5, 1)
        List<UserDTO> userDTOList = userService.query()
                .in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        // 4.返回
        return Result.ok(userDTOList);
    }

    /**
     * 查询探店笔记用户
     * @Author israein
     * @date 20:51 2023/5/31
     * @param blog
     **/
    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
