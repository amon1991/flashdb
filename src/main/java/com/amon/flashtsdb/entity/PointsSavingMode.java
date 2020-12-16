package com.amon.flashtsdb.entity;

/**
 * @author yaming.chen@foxmail.com
 * Created by chenyaming on 2020/10/27.
 */
public enum PointsSavingMode {

    /**
     * cover mode
     */
    COVER {
        @Override
        public Integer getMode() {
            return 0;
        }
    }
    /**
     * merge mode
     */
    , MERGE {
        @Override
        public Integer getMode() {
            return 1;
        }
    };

    public abstract Integer getMode();

}
