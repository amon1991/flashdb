package com.amon.flashtsdb.hbase;

import com.amon.flashtsdb.entity.FlashRowkey;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.MD5Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * This class is designed for hbase rowkey convert with tag
 *
 * @author yaming.chen@foxmail.com
 * Created by chenyaming on 2020/1/19.
 */
@Service
public class RowKeyUtil {

    private static final long bgIndex = 1000L;

    public static final String TAGID_INDEX = "TAGID_INDEX:";
    private static final String TAGID_INDEX_CURRENT = "TAGID_INDEX_CURRENT";
    private static final int ROWKEY_LENGTH = 12;

    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    public RowKeyUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * convert tag to rowkey bytes
     *
     * @param tag
     * @param timestamp timestamp of day
     * @return bytes = hash(4 byte) + tagNumId(4 byte) + timestamp(4 byte)
     */
    public byte[] tag2Rowkey(String tag, Integer timestamp) {

        if (null != tag) {

            Integer tagNumId;

            // computing tagNumId
            String tagId = stringRedisTemplate.opsForValue().get(TAGID_INDEX + tag);
            if (null != tagId) {
                tagNumId = Integer.valueOf(tagId);
            } else {
                Long newId = stringRedisTemplate.opsForValue().increment(TAGID_INDEX_CURRENT);
                stringRedisTemplate.opsForValue().set(TAGID_INDEX + tag, newId + "");
                tagNumId = Math.toIntExact(newId);
            }

            byte[] prefixBytes = Bytes.toBytes(MD5Hash.getMD5AsHex(Bytes.toBytes(tagNumId)).substring(0, 4));
            byte[] rowkey = Bytes.add(prefixBytes, intToBytes(tagNumId), intToBytes(timestamp));
            return rowkey;

        } else {
            return null;
        }

    }

    /**
     * convert rowkey bytes to flash rowkey object
     *
     * @param rowkey rowkey length must be 12
     * @return
     */
    public FlashRowkey rowkey2FlashRowkey(byte[] rowkey) {

        if (null != rowkey && rowkey.length == ROWKEY_LENGTH) {

            Integer tagNumId = bytesToInt(rowkey, 4);
            Integer timestamp = bytesToInt(rowkey, 8);

            FlashRowkey flashRowkey = new FlashRowkey();
            flashRowkey.setTagId(tagNumId);
            flashRowkey.setTimestamp(timestamp);

            return flashRowkey;

        } else {
            return null;
        }

    }

    public static byte[] intToBytes(int i) {
        byte[] targets = new byte[4];
        targets[3] = (byte) (i & 0xFF);
        targets[2] = (byte) (i >> 8 & 0xFF);
        targets[1] = (byte) (i >> 16 & 0xFF);
        targets[0] = (byte) (i >> 24 & 0xFF);
        return targets;
    }

    public static int bytesToInt(byte[] bytes, int off) {
        int b0 = bytes[off] & 0xFF;
        int b1 = bytes[off + 1] & 0xFF;
        int b2 = bytes[off + 2] & 0xFF;
        int b3 = bytes[off + 3] & 0xFF;
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }

}
