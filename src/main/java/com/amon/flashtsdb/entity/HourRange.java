package com.amon.flashtsdb.entity;

/**
 * @author yaming.chen@foxmail.com
 * Created by chenyaming on 2020/10/24.
 */
public class HourRange {

    private long bgHour;
    private long endHour;

    public long getBgHour() {
        return bgHour;
    }

    public void setBgHour(long bgHour) {
        this.bgHour = bgHour;
    }

    public long getEndHour() {
        return endHour;
    }

    public void setEndHour(long endHour) {
        this.endHour = endHour;
    }

}
