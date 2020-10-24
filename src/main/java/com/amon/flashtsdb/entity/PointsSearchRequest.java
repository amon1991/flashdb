package com.amon.flashtsdb.entity;

import java.util.List;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2020/2/12.
 */
public class PointsSearchRequest {

    private List<String> tagList;
    /**
     * 0:raw data
     * 1:interpolated data
     */
    private Integer searchMode;
    private long bgTime;
    private long endTime;

    public PointsSearchRequest() {
    }

    public PointsSearchRequest(List<String> tagList, Integer searchMode, long bgTime, long endTime, Integer searchInterval) {
        this.tagList = tagList;
        this.searchMode = searchMode;
        this.bgTime = bgTime;
        this.endTime = endTime;
        this.searchInterval = searchInterval;
    }

    /**
     * search interval by interpolation algorithm
     */
    private Integer searchInterval;

    /**
     * if limit > 0 and searchMode is interpolated,the searchInterval will be auto-calculated by time range and limit num
     */
    private int limit = -1;

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public List<String> getTagList() {
        return tagList;
    }

    public void setTagList(List<String> tagList) {
        this.tagList = tagList;
    }

    public Integer getSearchMode() {
        return searchMode;
    }

    public void setSearchMode(Integer searchMode) {
        this.searchMode = searchMode;
    }

    public long getBgTime() {
        return bgTime;
    }

    public void setBgTime(long bgTime) {
        this.bgTime = bgTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public Integer getSearchInterval() {
        return searchInterval;
    }

    public void setSearchInterval(Integer searchInterval) {
        this.searchInterval = searchInterval;
    }
}
