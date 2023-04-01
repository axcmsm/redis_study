package com.axcmsm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.axcmsm.entity.User;
import com.axcmsm.mapper.UserMapper;
import com.axcmsm.service.IUserService;
import org.springframework.stereotype.Service;


@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

}
