package com.amon.flashtsdb.sdt;

/**
 * @author yaming.chen@foxmail.com
 * Created by chenyaming on 2019/11/27.
 */
public class SdtPoints {

    /**
     * begin point
     */
    private Point beginPoint;
    /**
     * last point
     */
    private Point lastPoint;


    public Point getBeginPoint() {
        return beginPoint;
    }

    public void setBeginPoint(Point beginPoint) {
        this.beginPoint = beginPoint;
    }

    public Point getLastPoint() {
        return lastPoint;
    }

    public void setLastPoint(Point lastPoint) {
        this.lastPoint = lastPoint;
    }

}
