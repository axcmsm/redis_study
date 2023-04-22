package com.axcmsm.config;

import com.axcmsm.utils.LoginInterceptor;
import com.axcmsm.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * this class is for Axcmsm
 *
 * @author 须贺
 * @version 2023/4/2 10:58
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //默认的order是0;按照添加顺序执行
        //拦截所有请求
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).addPathPatterns("/**").order(0);
        //拦截部分请求
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/code","/api/user/login","/api/blog/hot",
                        "/api/shop/**",
                        "/api/shop-type/**",
                        "/api/upload/**",
                        "/api/voucher/**"
                ).order(1);
      }
}
