package com.axcmsm.service.impl;

import com.axcmsm.dto.Result;
import com.axcmsm.utils.UserHolder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.axcmsm.entity.Follow;
import com.axcmsm.mapper.FollowMapper;
import com.axcmsm.service.IFollowService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result follow(Long id, Boolean isFollow) {
        Long userId = UserHolder.getUser().getId();
        if (isFollow) {
            //关注
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(id);
            save(follow);
        } else {
            //取关 delete from tb_follow where user_id=? and follow_user_id=?
            remove(new QueryWrapper<Follow>().eq("user_id", userId).eq("follow_user_id", id));
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long id) {
        Long userId = UserHolder.getUser().getId();
        Integer one = query().eq("user_id", userId).eq("follow_user_id", id).count();
        return Result.ok(one > 0);
    }
}
