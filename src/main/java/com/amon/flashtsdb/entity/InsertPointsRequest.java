package com.amon.flashtsdb.entity;

import java.util.List;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2020/10/27.
 */
public class InsertPointsRequest {

    /**
     * COVER or MERGE MODE
     */
    private Integer savingMode;

    List<TagPointList> tagPointLists;

    public Integer getSavingMode() {
        return savingMode;
    }

    public void setSavingMode(Integer savingMode) {
        this.savingMode = savingMode;
    }

    public List<TagPointList> getTagPointLists() {
        return tagPointLists;
    }

    public void setTagPointLists(List<TagPointList> tagPointLists) {
        this.tagPointLists = tagPointLists;
    }

}
