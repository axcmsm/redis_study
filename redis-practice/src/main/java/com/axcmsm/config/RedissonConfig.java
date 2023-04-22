package com.axcmsm.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName: com.axcmsm.config.RedissonConfig
 * 微信公众号：代码飞快
 * Description:
 * 配置Redisson
 * @author 须贺
 * @version 2023/4/22
 */
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(){
        //配置
        Config config = new Config();
        //创建redisson对象
//        config.useClusterServers()//集群模式
        config.useSingleServer().setAddress("redis://10.32.30.60:6379").setDatabase(0);//单机模式
        return Redisson.create(config);
    }
}
