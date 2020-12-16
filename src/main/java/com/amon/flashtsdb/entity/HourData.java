package com.amon.flashtsdb.entity;

import com.amon.flashtsdb.sdt.Point;

import java.util.List;

/**
 * @author yaming.chen@foxmail.com
 * Created by chenyaming on 2020/1/21.
 */
public class HourData {

    private Integer qualifier;
    private List<Point> pointList;

    public Integer getQualifier() {
        return qualifier;
    }

    public void setQualifier(Integer qualifier) {
        this.qualifier = qualifier;
    }

    public List<Point> getPointList() {
        return pointList;
    }

    public void setPointList(List<Point> pointList) {
        this.pointList = pointList;
    }

}
