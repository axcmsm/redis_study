package com.axcmsm.service;

import com.axcmsm.dto.Result;
import com.baomidou.mybatisplus.extension.service.IService;
import com.axcmsm.entity.Follow;


public interface IFollowService extends IService<Follow> {

    Result follow(Long id, Boolean isFollow);

    Result isFollow(Long id);
}
