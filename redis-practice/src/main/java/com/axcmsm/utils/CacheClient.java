package com.axcmsm.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.axcmsm.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * this class is for Axcmsm
 *
 * @author 须贺
 * @version 2023/4/3 10:19
 */
@Slf4j
@Component
public class CacheClient {
    //@Resource
    private final StringRedisTemplate stringRedisTemplate;
    public CacheClient(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate=stringRedisTemplate;
    }


    /**
     * 设置值
     * @param key
     * @param value
     * @param time
     * @param unit
     */
    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }




    /**
     * 解决缓存穿透问题  缓存空对象
     * @param keyPrefix
     * @param id
     * @param type
     * @param dbFallback
     * @param time
     * @param unit
     * @param <R>
     * @param <ID>
     * @return
     */
    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback,Long time, TimeUnit unit){
        //1. 查询缓存
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        //2. 命中返回
        if(StrUtil.isNotBlank(json)){
            return JSONUtil.toBean(json, type);
        }
        //判断空值
        if(Objects.equals(json, "")){
            return null;
        }
        //3. 未命中查询数据库，写入缓存
        R data = dbFallback.apply(id);//传递式编程
        if(data==null){
            //将空值写入Redis 防止击穿问题
            stringRedisTemplate.opsForValue().set(key,"", 2,TimeUnit.MINUTES);
            return null;
        }
        this.set(key,data,time,unit);
        //4. 返回数据
        return data;
    }
    /**
     * 解决缓存击穿问题 逻辑过期 数据预热
     * @param key
     * @param value
     * @param time
     * @param unit
     */
    public void setWithLogicalExpire(String key, Object value,Long time, TimeUnit unit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    //逻辑过期
    public <R,ID> R queryWithLogicalExpire(String keyPrefix, ID id,Class<R> type,Function<ID,R> dbFallback,Long time, TimeUnit unit){
        //1. 查询缓存
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        //2. 未命中返回null
        if(StrUtil.isBlank(json)){
            return null;
        }
        //3. 命中判断时间是否过期
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R data = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())){
            //如果未过期
            return data;
        }
        //已过期，缓存构建
        //互斥锁
        String  lockkey=RedisConstants.LOCK_SHOP_KEY+id;
        boolean isLock = tryLock(lockkey);
        if (isLock){
            //为什么要做DoubleCheck? 在高并发多线程的情况下，不排除，在释放锁的一瞬间，数据还没来得及更新，另一个线程抢到锁，又再次进行缓存构建。
            //再次检查缓存是否过期 DoubleCheck
            json = stringRedisTemplate.opsForValue().get(key);
            //2. 未命中返回null
            if(StrUtil.isBlank(json)){
                return null;
            }
            //3. 命中判断时间是否过期
            redisData = JSONUtil.toBean(json, RedisData.class);
            data = JSONUtil.toBean((JSONObject) redisData.getData(), type);
            expireTime = redisData.getExpireTime();
            if (expireTime.isAfter(LocalDateTime.now())){
                //如果未过期
                return data;
            }
            // 开启独立线程，实现缓存重建
            CHCHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    //缓存重建
                    R result = dbFallback.apply(id);//查询
                    this.setWithLogicalExpire(key,result,time,unit);//写入redis
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unLock(lockkey);
                }

            });
        }
        return data;
    }
    //企业开发需要手动创建线程池
    private static final ExecutorService CHCHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);
    //setnx  枷锁
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }
    //setnx  释放嗦
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }
}
