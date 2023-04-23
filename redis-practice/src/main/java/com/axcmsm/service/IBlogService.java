package com.axcmsm.service;

import com.axcmsm.dto.Result;
import com.baomidou.mybatisplus.extension.service.IService;
import com.axcmsm.entity.Blog;


public interface IBlogService extends IService<Blog> {

    Result queryBlogById(Long id);

    Result like(Long id);

    Result queryHotBlog(Integer current);

    Result likes(Long id);

    Result saveBlog(Blog blog);

    Result queryBlogOfFollow(Long max, Integer offset);
}
