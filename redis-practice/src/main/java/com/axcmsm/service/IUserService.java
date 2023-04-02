package com.axcmsm.service;

import com.axcmsm.dto.LoginFormDTO;
import com.axcmsm.dto.Result;
import com.baomidou.mybatisplus.extension.service.IService;
import com.axcmsm.entity.User;

import javax.servlet.http.HttpSession;


public interface IUserService extends IService<User> {
    Result sendCode(String phone, HttpSession session);
    Result login(LoginFormDTO loginForm, HttpSession session);
}
