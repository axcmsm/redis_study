package com.axcmsm.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.axcmsm.dto.Result;
import com.axcmsm.utils.CacheClient;
import com.axcmsm.utils.RedisConstants;
import com.axcmsm.utils.RedisData;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.axcmsm.entity.Shop;
import com.axcmsm.mapper.ShopMapper;
import com.axcmsm.service.IShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        //缓存穿透
        //Shop shop = queryWithPassThrough(id);
        //Shop shop =cacheClient.queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, 1L, TimeUnit.DAYS);
        //缓存击穿 （互斥锁）
        //Shop shop = queryWithMutex(id);
        // 缓存击穿 (使用逻辑过期解决)
        //Shop shop = queryWithLogicalExpire(id);
        Shop shop = cacheClient.queryWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, 1L, TimeUnit.DAYS);
        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        return Result.ok(shop);
    }


    /**
     * 互斥锁 解决缓存击穿
     * @param id
     * @return
     */
    public Shop queryWithMutex(Long id){
        //1. 查询缓存
        //hash相对value存储的数据量小一些. 这里为了方便使用了value类型
        //String cache_shop_key = "cache:shop:" + id;
        String cache_shop_key = RedisConstants.CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(cache_shop_key);
        //2. 命中返回
        if(StrUtil.isNotBlank(shopJson)){
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        //判断命中的是否为空值
        if(Objects.equals(shopJson, "")){
            return null;
        }
        //3 实现缓存重建
        String lockkey="lock:shop:"+id;
        Shop shop=null;
        try {
            boolean isLock = tryLock(lockkey);
            if (!isLock){//休眠，重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }

         //在获取到互斥锁之后，如果没有对Redis的存在与否进行判断，还是会有很大线程直接访问数据库的
        //再次检查缓存是否存在
        shopJson = stringRedisTemplate.opsForValue().get(cache_shop_key);
        if(StrUtil.isNotBlank(shopJson)){
            shop = JSONUtil.toBean(shopJson, Shop.class);
           return shop;
       }

            //4. 未命中查询数据库，写入缓存
            shop = getById(id);
            System.out.println(shop);
            if(shop==null){
                //将空值写入Redis 防止击穿问题
                stringRedisTemplate.opsForValue().set(cache_shop_key,"", 2,TimeUnit.MINUTES);
                return null;
            }
            stringRedisTemplate.opsForValue().set(cache_shop_key,JSONUtil.toJsonStr(shop), 1,TimeUnit.DAYS);

        }catch(InterruptedException e){
            throw new RuntimeException(e);
        }finally {
            // 释放 互斥锁
            unLock(lockkey);
        }
        //5. 返回数据
        return shop;
    }

    /**
     * 解决缓存穿透问题
     * @param id
     * @return
     */
    public Shop queryWithPassThrough(Long id){
        //1. 查询缓存
        //hash相对value存储的数据量小一些. 这里为了方便使用了value类型
        //String cache_shop_key = "cache:shop:" + id;
        String cache_shop_key = RedisConstants.CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(cache_shop_key);
        //2. 命中返回
        if(StrUtil.isNotBlank(shopJson)){
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        //判断空值
        if(Objects.equals(shopJson, "")){
            return null;
        }
        //3. 未命中查询数据库，写入缓存
        Shop shop = getById(id);
        if(shop==null){
            //将空值写入Redis 防止击穿问题
            stringRedisTemplate.opsForValue().set(cache_shop_key,"", 2,TimeUnit.MINUTES);
            return null;
        }
        stringRedisTemplate.opsForValue().set(cache_shop_key,JSONUtil.toJsonStr(shop), 1,TimeUnit.DAYS);
        //4. 返回数据
        return shop;
    }

    //setnx  枷锁
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }
    //setnx  释放嗦
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }


    /**
     * 数据预热
     * 针对热点key问题
     * 使用逻辑过期方式解决
     * 在写入数据时存储逻辑过期时间字段
     * @param id
     */
    private void saveShop2Redis(Long id,Long expireSeconds){
        //1. 查询店铺的数据
        Shop shop = getById(id);
        //2. 封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //3. 写入Redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

    public Shop queryWithLogicalExpire(Long id){
        //1. 查询缓存
        //String cache_shop_key = "cache:shop:" + id;
        String cache_shop_key = RedisConstants.CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(cache_shop_key);
        //2. 未命中返回null
        if(StrUtil.isBlank(shopJson)){
            return null;
        }
        //3. 命中判断时间是否过期
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        Shop shop = JSONUtil.toBean(data, Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())){
            //如果未过期
            return shop;
        }
        //已过期，缓存构建
        //互斥锁
        String  lockkey="lock:shop:"+id;
        boolean isLock = tryLock(lockkey);
        if (isLock){
            //为什么要做DoubleCheck? 在高并发多线程的情况下，不排除，在释放锁的一瞬间，数据还没来得及更新，另一个线程抢到锁，又再次进行缓存构建。
            //再次检查缓存是否过期 DoubleCheck
             shopJson = stringRedisTemplate.opsForValue().get(cache_shop_key);
            //2. 未命中返回null
            if(StrUtil.isBlank(shopJson)){
                return null;
            }
            redisData = JSONUtil.toBean(shopJson, RedisData.class);
            shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
            expireTime = redisData.getExpireTime();
            if (expireTime.isAfter(LocalDateTime.now())){
                //如果未过期
                return shop;
            }

            // 开启独立线程，实现缓存重建
            CHCHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    //缓存重建
                    this.saveShop2Redis(id,30L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unLock(lockkey);
                }

            });
        }
        // 返回过期店铺数据



        return shop;
    }

    //企业开发需要手动创建线程池
    private static final ExecutorService CHCHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);



    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id==null){
            return Result.fail("店铺id不存在，无法更新");
        }
        //更新缓存
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
