package com.axcmsm.test;

import com.axcmsm.jedis.util.JedisConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

/**
 * this class is for Axcmsm
 *
 * @author 须贺
 * @version 2023/4/1 17:52
 */
public class JedisTest {
    private Jedis jedis;

    @BeforeEach
    void setUp(){
//        jedis=new Jedis("master",6379);
        jedis= JedisConnectionFactory.getJedis();
        // jedis.auth("passwd");//密码
        jedis.select(0);//选择库
    }

    @Test
    void TestString(){
        //设置数据
        String result = jedis.set("test:name", "张三");
        System.out.println(result);

        //获取数据
        String name = jedis.get("test:name");
        System.out.println(name);
    }

    @AfterEach
    void close(){
        if(jedis!=null){jedis.close();}
    }
}
