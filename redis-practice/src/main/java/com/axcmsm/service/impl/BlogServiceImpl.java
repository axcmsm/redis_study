package com.axcmsm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.axcmsm.dto.Result;
import com.axcmsm.dto.ScrollResult;
import com.axcmsm.dto.UserDTO;
import com.axcmsm.entity.Follow;
import com.axcmsm.entity.User;
import com.axcmsm.service.IFollowService;
import com.axcmsm.service.IUserService;
import com.axcmsm.utils.RedisConstants;
import com.axcmsm.utils.SystemConstants;
import com.axcmsm.utils.UserHolder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.axcmsm.entity.Blog;
import com.axcmsm.mapper.BlogMapper;
import com.axcmsm.service.IBlogService;
import org.aspectj.weaver.ast.Var;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService iUserService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IFollowService iFollowService;

    @Override
    public Result queryBlogById(Long id) {
        //查询blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("没有该博文");
        }
        //查询用户信息 封装信息
        queryBlogUser(blog);
        //查询blog是否被点赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    private void isBlogLiked(Blog blog) {
        String key = RedisConstants.BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, blog.getUserId().toString());
        blog.setIsLike(score != null);
    }

    @Override
    public Result like(Long id) {
        // 获取登录用户
        Long user_id = UserHolder.getUser().getId();
        // 判断当前用户是否已经点赞
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, user_id.toString());

        // 如果未点赞，可以点赞
        if (score == null) {
            // 数据库点赞+1
            boolean isSuccess = update().setSql("liked = liked + 1 ").eq("id", id).update();
            // 保存用户到redis的set集合  zdd  key value score
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(key, user_id.toString(), System.currentTimeMillis());
            }
        } else {
            // 如果未点赞，取消点赞
            // 数据库点赞-1
            boolean isSuccess = update().setSql("liked = liked - 1 ").eq("id", id).update();
            // 把用户从Redis的set集合移除
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().remove(key, user_id.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            queryBlogUser(blog);
            isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result likes(Long id) {
        // TODO: 2023/4/23 Top5的点赞用户 zrange key 0 4
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5 == null || top5.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idsStr = StrUtil.join(",", ids);
        List<UserDTO> dtoList = iUserService.query().in("id", ids).last("order by FIELD(id," + idsStr + ")").list()
                .stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(dtoList);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean isSuccess = save(blog);
        if (!isSuccess) {
            return Result.fail("新增失败！");
        }

        // 查询笔记作者的所有粉丝
        List<Follow> follows = iFollowService.query().eq("follow_user_id", user.getId()).list();
        // 推送笔记id给所有粉丝
        for (Follow follow : follows) {
            Long userId = follow.getUserId();
            String key = "feed:" + userId;
            stringRedisTemplate.opsForZSet().add(key, blog.getUserId().toString(), System.currentTimeMillis());
        }

        // 返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        //1. 查询当前用户
        Long userId = UserHolder.getUser().getId();
        //2. 查询收件箱 zrevrangebyscore key max min  limit offset count
        String key = RedisConstants.FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typeTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        if (typeTuples == null || typeTuples.isEmpty()) {
            return Result.ok();
        }

        //3. 解析数据：blogId,minTime,offset
        ArrayList<Long> ids = new ArrayList<>(typeTuples.size());
        long minTime = 0;
        int os = 1;
        for (ZSetOperations.TypedTuple<String> tuple : typeTuples) {
            ids.add(Long.valueOf(tuple.getValue()));//获取id
            long time = tuple.getScore().longValue();//分数（时间戳）
            if (time == minTime) {
                os++;
            } else {
                minTime = time;
                os = 1;
            }
        }
        String idsStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("order by FIELD(id," + idsStr + ")").list();
        for (Blog blog : blogs) {
            queryBlogUser(blog);//查询有关用户，封装
            isBlogLiked(blog);//查询是否被点赞
        }
        //4. 封装数据
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(os);
        scrollResult.setMinTime(minTime);
        return Result.ok(scrollResult);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = iUserService.getById(userId);
        blog.setName(user.getNickName());
        blog.setName(user.getIcon());
    }
}
