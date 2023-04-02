package com.axcmsm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.axcmsm.dto.LoginFormDTO;
import com.axcmsm.dto.Result;
import com.axcmsm.dto.UserDTO;
import com.axcmsm.entity.UserInfo;
import com.axcmsm.utils.RegexUtils;
import com.axcmsm.utils.SystemConstants;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.axcmsm.entity.User;
import com.axcmsm.mapper.UserMapper;
import com.axcmsm.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.axcmsm.utils.RedisConstants.*;


@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {


    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1. 验证手机号
        if (RegexUtils.isPhoneInvalid(phone)){
            //2. 如果不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        //3. 符合，生成验证码
        String code = RandomUtil.randomNumbers(6);
        //4. 保证验证码到session
        //session.setAttribute("code",code);
        //"login:code:"  有效期，1分钟后
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code, Duration.ofMinutes(LOGIN_CODE_TTL));
        //5. 发送验证码
        log.info("发送短信验证码成功，验证码：{}",code);
        //6. 返回ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1. 验证手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)){
            //2. 如果不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        // 1.5 检验验证码
        String phone_code_key = LOGIN_CODE_KEY + phone;
        String cacheCode = stringRedisTemplate.opsForValue().get(phone_code_key);
        String code = loginForm.getCode();
        if(cacheCode==null||!cacheCode.equals(code)){
            return Result.fail("验证码错误");
        }

        //2. 根据手机号查询用户
        User user = query().eq("phone", phone).one();

        //3. 如果用户存在登录
        if(user==null){
            //4. 不存在注册
            user=create_user(phone);
        }

        //4.5 生成token => 这里没有使用jwt，可以扩展
        String token = UUID.randomUUID().toString();
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //转成map格式，并对对象里面的数据类型全转成string类型
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor((name,value)->value.toString())
        );

        //5. 保存用户信息到session
        //可以使用String类型以json格式存储，比较直观
        //可以使用hash结构对每个字段进行独立存储，可以针对单个字段CRUD,并且内存占用较少
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);
        stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL, TimeUnit.MINUTES);
        stringRedisTemplate.delete(phone_code_key);//登录成功删除验证码
        return Result.ok(tokenKey);
    }

    private User create_user(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX +RandomUtil.randomString(10));
        save(user);
        return query().eq("phone", phone).one();
    }
}
