package com.amon.flashtsdb.entity;

import java.util.List;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2020/1/20.
 */
public class SplitTagPointList {

    private String tag;

    private List<DayData> dayDataList;

    private Double accuracyE;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public List<DayData> getDayDataList() {
        return dayDataList;
    }

    public void setDayDataList(List<DayData> dayDataList) {
        this.dayDataList = dayDataList;
    }

    public Double getAccuracyE() {
        return accuracyE;
    }

    public void setAccuracyE(Double accuracyE) {
        this.accuracyE = accuracyE;
    }

}
