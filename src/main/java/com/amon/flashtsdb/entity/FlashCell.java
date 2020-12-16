package com.amon.flashtsdb.entity;

import java.io.Serializable;

/**
 * @author yaming.chen@foxmail.com
 * Created by chenyaming on 2020/1/19.
 */
public class FlashCell implements Serializable {

    private static final long serialVersionUID = 1L;

    private String columnFamily;
    private String column;
    private long timestamp;
    private String value;

    @Override
    public String toString() {
        return "Row{" +
                ", columnFamily='" + columnFamily + '\'' +
                ", column='" + column + '\'' +
                ", timestamp=" + timestamp +
                ", value='" + value + '\'' +
                '}';
    }


    public String getColumnFamily() {
        return columnFamily;
    }

    public void setColumnFamily(String columnFamily) {
        this.columnFamily = columnFamily;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
