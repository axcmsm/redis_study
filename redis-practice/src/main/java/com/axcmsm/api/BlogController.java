package com.axcmsm.api;


import com.axcmsm.dto.Result;
import com.axcmsm.dto.UserDTO;
import com.axcmsm.entity.Blog;
import com.axcmsm.entity.User;
import com.axcmsm.service.IBlogService;
import com.axcmsm.service.IUserService;
import com.axcmsm.utils.SystemConstants;
import com.axcmsm.utils.UserHolder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/blog")
public class BlogController {

//    @Resource
    @Autowired
    private IBlogService blogService;
//    @Resource
    @Autowired
    private IUserService userService;

    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }

    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        return blogService.like(id);
    }

    @GetMapping("/likes/{id}")
    public Result likes(@PathVariable("id") Long id) {
        return blogService.likes(id);
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {

        return blogService.queryHotBlog(current);
    }

    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id){
        return blogService.queryBlogById(id);
    }


    @GetMapping("/of/user")
    public Result queryByUserId(
            @RequestParam(value = "current",defaultValue = "1") Integer current,
            @RequestParam("id") Long id
    ){
        Page<Blog> page = blogService.query().eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }


    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(
            @RequestParam("lastId") Long max,
            @RequestParam(value = "offset",defaultValue = "0") Integer offset
    ){
        return blogService.queryBlogOfFollow(max,offset);
    }
}
