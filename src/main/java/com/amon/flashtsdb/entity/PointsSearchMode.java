package com.amon.flashtsdb.entity;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2018/3/22.
 */
public enum PointsSearchMode {

    /**
     * original data
     */
    RAW {
        @Override
        public Integer getMode() {
            return 0;
        }
    }
    /**
     * interpolated data
     */
    ,INTERPOLATED  {
        @Override
        public Integer getMode() {
            return 1;
        }
    };

    public abstract Integer getMode();

}
