package com.amon.flashtsdb.threadpool.pointshistorcalsearch;

import com.alibaba.fastjson.JSON;
import com.amon.flashtsdb.entity.FlashRowkey;
import com.amon.flashtsdb.entity.HourRange;
import com.amon.flashtsdb.entity.PointsSearchMode;
import com.amon.flashtsdb.entity.TagPointList;
import com.amon.flashtsdb.hbase.HBaseUtil;
import com.amon.flashtsdb.hbase.RowKeyUtil;
import com.amon.flashtsdb.sdt.Point;
import com.amon.flashtsdb.sdt.SdtPeriod;
import com.amon.flashtsdb.sdt.SdtPoints;
import com.amon.flashtsdb.sdt.SdtService;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2020/10/26.
 */
public class SearchTask implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private CountDownLatch cdl;

    private Integer searchMode;
    private long bgTime;
    private long endTime;
    private Integer searchInterval;
    private int limit;
    private boolean limited;
    private Integer bgDayTimestamp;
    private Integer endDayTimestamp;
    private Map<String, HourRange> searchScopeMap;
    private TagPointList tagPointList;
    private String tag;
    private HBaseUtil hBaseUtil;
    private RowKeyUtil rowKeyUtil;
    private SdtService sdtService;
    private String hbaseTableName;
    private final static int DAY_HOURS = 24;
    private static final String defaultColumnFamily = "t";


    public SearchTask(CountDownLatch cdl, Integer searchMode, long bgTime, long endTime, Integer searchInterval, int limit, boolean limited, Integer bgDayTimestamp, Integer endDayTimestamp, Map<String, HourRange> searchScopeMap, TagPointList tagPointList, String tag, HBaseUtil hBaseUtil, RowKeyUtil rowKeyUtil, SdtService sdtService, String hbaseTableName) {
        this.cdl = cdl;
        this.searchMode = searchMode;
        this.bgTime = bgTime;
        this.endTime = endTime;
        this.searchInterval = searchInterval;
        this.limit = limit;
        this.limited = limited;
        this.bgDayTimestamp = bgDayTimestamp;
        this.endDayTimestamp = endDayTimestamp;
        this.searchScopeMap = searchScopeMap;
        this.tagPointList = tagPointList;
        this.tag = tag;
        this.hBaseUtil = hBaseUtil;
        this.rowKeyUtil = rowKeyUtil;
        this.sdtService = sdtService;
        this.hbaseTableName = hbaseTableName;
    }

    @Override
    public void run() {

        if (endDayTimestamp > bgDayTimestamp) {
            // hbase scan will not include endDayTimestamp,so the endday should add one
            endDayTimestamp++;
        }

        byte[] startRowkey = rowKeyUtil.tag2Rowkey(tag, bgDayTimestamp);
        byte[] endRowkey = rowKeyUtil.tag2Rowkey(tag, endDayTimestamp);


        List<SdtPeriod> mergedSdtPeriodList = new ArrayList<>();

        try {

            ResultScanner resultScanner = hBaseUtil.scanByStartAndStopRowKey(hbaseTableName, startRowkey, endRowkey);

            Iterator<Result> iterator = resultScanner.iterator();

            SdtPeriod lastSdtPeriod = null;
            while (iterator.hasNext()) {

                Result myresult = iterator.next();
                FlashRowkey flashRowkey = rowKeyUtil.rowkey2FlashRowkey(myresult.getRow());
                int dayTime = flashRowkey.getTimestamp();
                HourRange hourRange = searchScopeMap.get(dayTime + "");
                for (int i = 0; i < DAY_HOURS; i++) {
                    if (null != hourRange && hourRange.getBgHour() <= i && i <= hourRange.getEndHour()) {
                        List<SdtPeriod> sdtPeriodList = JSON.parseArray(hBaseUtil.getValueByResult(myresult,
                                defaultColumnFamily, i + ""), SdtPeriod.class);
                        if (null != sdtPeriodList && sdtPeriodList.size() > 0) {
                            if (lastSdtPeriod != null) {
                                // add std period by lastSdtPeriod and this hour's first stdPeriod
                                SdtPoints sdtPoints = new SdtPoints();
                                Point beginPoint = new Point();
                                beginPoint.setX(lastSdtPeriod.getEndTime());
                                beginPoint.setY(lastSdtPeriod.getEndValue());
                                Point lastPoint = new Point();
                                lastPoint.setX(sdtPeriodList.get(0).getBgTime());
                                lastPoint.setY(sdtPeriodList.get(0).getBgValue());
                                sdtPoints.setBeginPoint(beginPoint);
                                sdtPoints.setLastPoint(lastPoint);
                                // add std period
                                mergedSdtPeriodList.add(sdtService.structSdtPeriod(sdtPoints));
                            }
                            mergedSdtPeriodList.addAll(sdtPeriodList);
                            // set last period
                            lastSdtPeriod = sdtPeriodList.get(sdtPeriodList.size() - 1);
                        }
                    }
                }

            }

            // uncompress point data
            if (searchMode.intValue() == PointsSearchMode.INTERPOLATED.getMode().intValue()) {
                tagPointList.setPointList(sdtService.sdtUnCompress(mergedSdtPeriodList, bgTime, endTime, searchInterval.longValue() * 1000L));
            } else if (searchMode.intValue() == PointsSearchMode.RAW.getMode().intValue()) {

                List<Point> pointList = new ArrayList<>();
                tagPointList.setPointList(pointList);

                if (null != mergedSdtPeriodList && mergedSdtPeriodList.size() > 0) {

                    int currentNum = 0;

                    for (SdtPeriod sdtPeriod : mergedSdtPeriodList) {

                        if (sdtPeriod.getBgTime() >= bgTime && sdtPeriod.getBgTime() <= endTime) {

                            Point bgPoint = new Point();
                            bgPoint.setX(sdtPeriod.getBgTime());
                            bgPoint.setY(sdtPeriod.getBgValue());

                            if (limited) {
                                if (limit > currentNum++) {
                                    pointList.add(bgPoint);
                                } else {
                                    break;
                                }
                            } else {
                                pointList.add(bgPoint);
                            }

                        }

                    }

                }

            }


        } catch (Exception e) {
            logger.error("Hbase scan data error:{}", e.getMessage(), e);
            e.printStackTrace();
        } finally {
            cdl.countDown();
        }
    }
}
