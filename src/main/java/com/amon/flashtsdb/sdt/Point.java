package com.amon.flashtsdb.sdt;

/**
 * @author yaming.chen@foxmail.com
 * Created by chenyaming on 2019/11/27.
 */
public class Point implements Comparable<Point> {

    private long x;
    private double y;

    public long getX() {
        return x;
    }

    public void setX(long x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public Point(long x, double y) {
        this.x = x;
        this.y = y;
    }

    public Point() {
    }

    @Override
    public int compareTo(Point point) {
        return Long.valueOf(x).compareTo(Long.valueOf(point.x));
    }
}
