package com.amon.flashtsdb.entity;

/**
 * @author yaming.chen@foxmail.com
 * Created by chenyaming on 2020/1/20.
 */
public class FlashRowkey {

    private Integer tagId;
    private Integer timestamp;

    public Integer getTagId() {
        return tagId;
    }

    public void setTagId(Integer tagId) {
        this.tagId = tagId;
    }

    public Integer getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Integer timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "FlashRowkey{" +
                "tagId=" + tagId +
                ", timestamp=" + timestamp +
                '}';
    }
}
