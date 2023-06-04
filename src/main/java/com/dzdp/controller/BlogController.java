package com.dzdp.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dzdp.dto.Result;
import com.dzdp.dto.UserDTO;
import com.dzdp.entity.Blog;
import com.dzdp.entity.User;
import com.dzdp.service.IBlogService;
import com.dzdp.service.IUserService;
import com.dzdp.utils.SystemConstants;
import com.dzdp.utils.UserHolder;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 探店笔记控制器
 * @Author israein
 * @date 19:36 2023/5/31
 **/
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;

    /**
     * 保存探店笔记
     * @Author israein
     * @date 19:46 2023/5/31
     * @param blog
     * @return com.dzdp.dto.Result
     **/
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }

    /**
     * 根据id查询探店笔记
     * @Author israein
     * @date 20:34 2023/5/31
     * @param id
     * @return com.dzdp.dto.Result
     **/
    @GetMapping("/{id}")
    public Result queryBlog(@PathVariable("id") Long id) {
        return blogService.queryBlogById(id);
    }


    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        return blogService.likeBlog(id);
    }

    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long id) {
        return blogService.queryBlogLikes(id);
    }

    /**
     * 根据id查询博主的探店笔记
     * @Author israein
     * @date 15:36 2023/6/4
     * @param current
     * @param id
     * @return com.dzdp.dto.Result
     **/
    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("id") Long id) {
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryMyBlog(current);
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }

    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(
            @RequestParam("lastId") Long max, @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        return blogService.queryBlogOfFollow(max, offset);
    }
}
