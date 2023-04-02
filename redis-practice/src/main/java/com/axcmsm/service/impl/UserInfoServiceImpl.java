package com.axcmsm.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.axcmsm.dto.LoginFormDTO;
import com.axcmsm.dto.Result;
import com.axcmsm.entity.User;
import com.axcmsm.utils.RegexUtils;
import com.axcmsm.utils.SystemConstants;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.axcmsm.entity.UserInfo;
import com.axcmsm.mapper.UserInfoMapper;
import com.axcmsm.service.IUserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;


@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
