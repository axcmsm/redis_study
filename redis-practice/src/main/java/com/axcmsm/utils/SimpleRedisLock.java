package com.axcmsm.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.TimeUnit;


/**
 * ClassName: com.axcmsm.utils.SimpleRedisLock
 * 微信公众号：代码飞快
 * Description:
 * 分布式的实现方案
 * @author 须贺
 * @version 2023/4/22
 */
public class SimpleRedisLock implements ILock{


    private String name;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }


    public static final String KEY_PREFIX="lock:";
    public static final String ID_PREFIX= UUID.randomUUID().toString(true)+"-";

    public static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        //类加载时，读取配置
        UNLOCK_SCRIPT=new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }


    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean tryLock(long timeoutSec) {
        //获取线程的标识
        String threadId = ID_PREFIX+Thread.currentThread().getId();

        //setnx
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);

        //这里是包装类有自动拆箱的过程，如果为空就不好了，所以可以推荐使用下面的方式返回。避免空指针的可能性。
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        //使用lua脚本释放锁
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX+name),
                ID_PREFIX+Thread.currentThread().getId()
        );
    }

    /*   @Override
    public void unlock() {
        //先获取线程标识，判断是否是所在jvm中的锁，防止误删
        String threadId = ID_PREFIX+Thread.currentThread().getId();
        String value = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        if(threadId.equals(value)){
            //释放锁
            stringRedisTemplate.delete(KEY_PREFIX+name);
        }
    }*/
}
