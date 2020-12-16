package com.amon.flashtsdb.redis;

import com.amon.flashtsdb.FlashtsdbApplication;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author yaming.chen@foxmail.com
 * Created by chenyaming on 2020/1/16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlashtsdbApplication.class)
public class RedisTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void testPutAndGet() {

        String key = "testKey";
        String value = "testValue";

        stringRedisTemplate.opsForValue().set(key, value);

        String valueFromRedis = stringRedisTemplate.opsForValue().get(key);

        Assert.assertEquals(value, valueFromRedis);

    }

}
