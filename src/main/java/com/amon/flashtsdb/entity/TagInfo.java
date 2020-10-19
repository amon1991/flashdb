package com.amon.flashtsdb.entity;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2020/2/17.
 */
public class TagInfo {

    private String tagCode;
    private String tagName;
    private String tagDescription;
    private String tagUnit;
    private double accuracyE;
    private long createtime;

    public TagInfo(String tagCode, String tagName, String tagDescription, String tagUnit, double accuracyE, long createtime) {
        this.tagCode = tagCode;
        this.tagName = tagName;
        this.tagDescription = tagDescription;
        this.tagUnit = tagUnit;
        this.accuracyE = accuracyE;
        this.createtime = createtime;
    }

    public String getTagCode() {
        return tagCode;
    }

    public void setTagCode(String tagCode) {
        this.tagCode = tagCode;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getTagDescription() {
        return tagDescription;
    }

    public void setTagDescription(String tagDescription) {
        this.tagDescription = tagDescription;
    }

    public String getTagUnit() {
        return tagUnit;
    }

    public void setTagUnit(String tagUnit) {
        this.tagUnit = tagUnit;
    }

    public long getCreatetime() {
        return createtime;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public double getAccuracyE() {
        return accuracyE;
    }

    public void setAccuracyE(double accuracyE) {
        this.accuracyE = accuracyE;
    }
}
