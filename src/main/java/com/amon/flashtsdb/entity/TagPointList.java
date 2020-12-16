package com.amon.flashtsdb.entity;

import com.amon.flashtsdb.sdt.Point;

import java.util.List;

/**
 * @author yaming.chen@foxmail.com
 * Created by chenyaming on 2020/1/21.
 */
public class TagPointList {

    private String tag;

    private List<Point> pointList;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public List<Point> getPointList() {
        return pointList;
    }

    public void setPointList(List<Point> pointList) {
        this.pointList = pointList;
    }

    @Override
    public String toString() {
        return "TagDataDTO{" +
                "tag='" + tag + '\'' +
                ", pointList=" + pointList +
                '}';
    }

}
