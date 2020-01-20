package com.amon.flashtsdb.hbase;

import com.amon.flashtsdb.FlashtsdbApplication;
import com.amon.flashtsdb.entity.FlashRowkey;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static com.amon.flashtsdb.hbase.RowKeyUtil.TAGID_INDEX;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2020/1/19.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlashtsdbApplication.class)
class RowKeyUtilTest {

    @Autowired
    private RowKeyUtil rowKeyUtil;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Test
    public void tag2Rowkey() {

        String tag = "@10MAC11CP001_XQ01";
        Integer timestamp = Math.toIntExact(System.currentTimeMillis() / (24 * 3600 * 1000L));
        byte[] rowKey = rowKeyUtil.tag2Rowkey(tag, timestamp);
        Assert.assertEquals(12, rowKey.length);

        FlashRowkey flashRowkey = rowKeyUtil.rowkey2FlashRowkey(rowKey);
        Assert.assertNotNull(flashRowkey);
        Integer tagNumId = null;
        String tagId = stringRedisTemplate.opsForValue().get(TAGID_INDEX + tag);
        if (null != tagId) {
            tagNumId = Integer.valueOf(tagId);
        }

        Assert.assertEquals(tagNumId, flashRowkey.getTagId());
        Assert.assertEquals(timestamp, flashRowkey.getTimestamp());

    }

}