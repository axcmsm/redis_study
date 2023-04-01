package com.axcmsm.redis;

import com.axcmsm.redis.pojo.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.jws.soap.SOAPBinding;

@SpringBootTest
class SpringBootDataRedisDemoApplicationTests {


    /*@Autowired
    private RedisTemplate redisTemplate;//jdk序列化*/
    @Autowired
    private StringRedisTemplate stringRedisTemplate;//StringrRedisTemplate
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;//配置序列化后的依赖

    private static final ObjectMapper mapper=new ObjectMapper();

    @Test
    void testString_1() {
        redisTemplate.opsForValue().set("data-test:name1","Axcmsm1");
        Object value = redisTemplate.opsForValue().get("data-test:name1");
        System.out.println(value);
    }


    @Test
    void testString_2() {
        stringRedisTemplate.opsForValue().set("data-test:name2","Axcmsm2");
        String res = stringRedisTemplate.opsForValue().get("data-test:name2");
        System.out.println(res);
    }

    @Test
    void testString_3() {
        redisTemplate.opsForValue().set("data-test:name3","Axcmsm3");
        Object value = redisTemplate.opsForValue().get("data-test:name3");
        System.out.println(value);
    }

    @Test
    void testString_user_redis() {
        User user = new User(1L, "张三", 12);
        redisTemplate.opsForValue().set("data-test:user1",user);
        User value = (User) redisTemplate.opsForValue().get("data-test:user1");
        System.out.println(value);
    }
    @Test //节省开销
    void testString_user_String() throws JsonProcessingException {
        User user = new User(2L, "李四", 13);
        String str = mapper.writeValueAsString(user);
        stringRedisTemplate.opsForValue().set("data-test:user2",str);
        String value = stringRedisTemplate.opsForValue().get("data-test:user2");
        User res = mapper.readValue(value, User.class);
        System.out.println(res);
    }


}
