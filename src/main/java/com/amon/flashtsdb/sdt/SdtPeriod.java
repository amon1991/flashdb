package com.amon.flashtsdb.sdt;

/**
 * @author yaming.chen@foxmail.com
 * Created by chenyaming on 2019/11/27.
 */
public class SdtPeriod {

    private long bgTime;
    private double bgValue;
    private long endTime;
    private double endValue;
    private double gradient;

    public long getBgTime() {
        return bgTime;
    }

    public void setBgTime(long bgTime) {
        this.bgTime = bgTime;
    }

    public double getBgValue() {
        return bgValue;
    }

    public void setBgValue(double bgValue) {
        this.bgValue = bgValue;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public double getEndValue() {
        return endValue;
    }

    public void setEndValue(double endValue) {
        this.endValue = endValue;
    }

    public double getGradient() {
        return gradient;
    }

    public void setGradient(double gradient) {
        this.gradient = gradient;
    }
}
