package com.axcmsm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.axcmsm.entity.Follow;
import com.axcmsm.mapper.FollowMapper;
import com.axcmsm.service.IFollowService;
import org.springframework.stereotype.Service;


@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

}
