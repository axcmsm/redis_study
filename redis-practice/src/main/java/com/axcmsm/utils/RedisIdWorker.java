package com.axcmsm.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * this class is for Axcmsm
 *
 * @author 须贺
 * @version 2023/4/3 11:09
 */
@Component
public class RedisIdWorker {
    //开始时间戳
    private static final long BEGIN_TIMESTAMP=1640995200L;
    //序列号位数
    private static final long COUNT_BITS=32;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     *  自增id+时间戳(31bit)+序列号(32bit)  => 二进制
     * @param keyPrefix
     * @return
     */
    public long nextId(String keyPrefix){
        // 1. 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        //2. 生成序列号
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        //3. 拼接
        return  timestamp << COUNT_BITS | count;
    }

}
