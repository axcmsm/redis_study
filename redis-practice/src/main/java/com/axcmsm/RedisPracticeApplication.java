package com.axcmsm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)//暴露代理对象
@SpringBootApplication
@MapperScan("com.axcmsm.mapper")
public class RedisPracticeApplication {
    public static void main(String[] args) {
        SpringApplication.run(RedisPracticeApplication.class, args);
    }
}
