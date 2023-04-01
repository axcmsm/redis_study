package com.axcmsm.jedis.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * this class is for Axcmsm
 *
 * @author 须贺
 * @version 2023/4/1 18:00
 */
public class JedisConnectionFactory {
    private static final JedisPool jedispool;
    static {
        //配置链接池
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(8);
        jedisPoolConfig.setMaxIdle(8);
        jedisPoolConfig.setMinIdle(0);
        jedisPoolConfig.setMaxWait(Duration.ofMillis(1000));
        //创建连接池对象
        jedispool = new JedisPool(jedisPoolConfig,"master",6379);
    }
    public static Jedis getJedis(){
        return jedispool.getResource();
    }

}
