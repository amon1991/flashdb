package com.amon.flashtsdb.entity;

import java.util.List;

/**
 * @author yaming.chen@foxmail.com
 * Created by chenyaming on 2020/1/21.
 */
public class DayData {

    private Integer timestamp;

    private List<HourData> hourDataList;

    public Integer getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Integer timestamp) {
        this.timestamp = timestamp;
    }

    public List<HourData> getHourDataList() {
        return hourDataList;
    }

    public void setHourDataList(List<HourData> hourDataList) {
        this.hourDataList = hourDataList;
    }
    
}
