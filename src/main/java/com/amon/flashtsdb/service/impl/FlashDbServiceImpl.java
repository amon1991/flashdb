package com.amon.flashtsdb.service.impl;

import com.alibaba.fastjson.JSON;
import com.amon.flashtsdb.entity.*;
import com.amon.flashtsdb.hbase.HBaseUtil;
import com.amon.flashtsdb.hbase.RowKeyUtil;
import com.amon.flashtsdb.sdt.Point;
import com.amon.flashtsdb.sdt.SdtPeriod;
import com.amon.flashtsdb.sdt.SdtService;
import com.amon.flashtsdb.service.FlashDbService;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2020/1/20.
 */
@Service
public class FlashDbServiceImpl implements FlashDbService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final static int DAY_HOURS = 24;

    @Value("${flashtsdb.config.tablename}")
    private String hbaseTableName;

    @Value("${flashtsdb.config.accuracy}")
    private Double defaultAccuracy;

    private static final String defaultColumnFamily = "t";

    private final HBaseUtil hBaseUtil;
    private final RowKeyUtil rowKeyUtil;
    private final SdtService sdtService;

    @Autowired
    public FlashDbServiceImpl(HBaseUtil hBaseUtil, RowKeyUtil rowKeyUtil, SdtService sdtService) {
        this.hBaseUtil = hBaseUtil;
        this.rowKeyUtil = rowKeyUtil;
        this.sdtService = sdtService;
    }

    @Override
    public int saveDataPoints(List<TagPointList> tagPointLists) {
        return dump2Hbase(convert2SplitTagPointList(tagPointLists));
    }

    /**
     * convert tagPointLists to SplitTagPointLists
     *
     * @param tagPointLists
     * @return
     */
    @Override
    public List<SplitTagPointList> convert2SplitTagPointList(List<TagPointList> tagPointLists) {

        if (null != tagPointLists && tagPointLists.size() > 0) {

            List<SplitTagPointList> splitTagPointLists = new ArrayList<>();

            for (TagPointList tagPointList : tagPointLists) {

                String tag = tagPointList.getTag();
                List<Point> pointList = tagPointList.getPointList();

                // sort point by timestamp first
                Collections.sort(pointList);

                SplitTagPointList splitTagPointList = new SplitTagPointList();
                splitTagPointLists.add(splitTagPointList);
                splitTagPointList.setTag(tag);

                List<DayData> dayDataList = new ArrayList<>();
                splitTagPointList.setDayDataList(dayDataList);

                // todo: get accuracyE in redis
                splitTagPointList.setAccuracyE(defaultAccuracy);

                int startDayTimestamp = 0;
                int startHourQualifier = -1;
                DayData dayData = null;
                HourData hourData = null;

                // split to day data
                for (Point point : pointList) {

                    Integer currnetDayTime = getDayTimeStamp(point.getX());

                    if (currnetDayTime>startDayTimestamp){
                        startDayTimestamp = currnetDayTime;
                        dayData = new DayData();
                        dayData.setTimestamp(currnetDayTime);
                        dayData.setHourDataList(new ArrayList<>());
                        dayDataList.add(dayData);
                    }

                    Integer currentHourQualifier = getHourQualifier(point.getX());

                    if (currentHourQualifier.intValue()!=startHourQualifier){
                        startHourQualifier = currentHourQualifier;
                        hourData = new HourData();
                        hourData.setQualifier(currentHourQualifier);
                        hourData.setPointList(new ArrayList<>());
                        dayData.getHourDataList().add(hourData);
                    }

                    hourData.getPointList().add(point);

                }

            }

            return splitTagPointLists;

        }
        return new ArrayList<>();
    }

    public Integer getDayTimeStamp(long timestamp){
        return Math.toIntExact(timestamp / (24 * 3600 * 1000L));
    }

    public Integer getHourQualifier(long timestamp){
        return Math.toIntExact(timestamp / (3600 * 1000L))%24;
    }


    /**
     * dump data to hbase
     *
     * @param dataList
     * @return
     */
    @Override
    public int dump2Hbase(@NotNull List<SplitTagPointList> dataList) {

        if (null != dataList && dataList.size() > 0) {

            Map<byte[], List<FlashCell>> dataMap = new HashMap<>(dataList.size() * 2);

            for (SplitTagPointList tagOriginalEntity : dataList) {

                // tag level
                double accuracyE = defaultAccuracy;
                if (null != tagOriginalEntity.getAccuracyE()) {
                    accuracyE = tagOriginalEntity.getAccuracyE();
                }

                String tag = tagOriginalEntity.getTag();

                List<DayData> dayDataList = tagOriginalEntity.getDayDataList();

                if (null != dayDataList && dayDataList.size() > 0) {

                    for (DayData dayData : dayDataList) {

                        // row level
                        List<HourData> hourDataList = dayData.getHourDataList();

                        if (null != hourDataList && hourDataList.size() > 0) {

                            byte[] rowkey = rowKeyUtil.tag2Rowkey(tag, dayData.getTimestamp());

                            List<FlashCell> flashCells = new ArrayList<>();

                            dataMap.put(rowkey, flashCells);

                            for (HourData hourData : hourDataList) {

                                // cell level
                                FlashCell flashCell = new FlashCell();
                                flashCell.setColumnFamily(defaultColumnFamily);
                                flashCell.setColumn(hourData.getQualifier() + "");
                                flashCell.setValue(JSON.toJSONString(sdtService.sdtCompress(hourData.getPointList(), accuracyE)));
                                flashCell.setTimestamp(System.currentTimeMillis());
                                flashCells.add(flashCell);

                            }

                        }

                    }

                }

            }

            try {
                return hBaseUtil.batchInsertRows(hbaseTableName, dataMap);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Batch insert rows failed,errorMsg:{}", e.getMessage(), e);
                return 0;
            }

        }

        return 0;
    }

    @Override
    public List<TagPointList> searchPoints(PointsSearchRequest pointsSearchRequest) {

        if (null!=pointsSearchRequest && null!=pointsSearchRequest.getSearchMode()){

            if (null!=pointsSearchRequest.getTagList() && pointsSearchRequest.getTagList().size()>0){

                List<String> tagList = pointsSearchRequest.getTagList();
                Integer searchMode = pointsSearchRequest.getSearchMode();
                long bgTime = pointsSearchRequest.getBgTime();
                long endTime = pointsSearchRequest.getEndTime();
                Integer searchInterval = pointsSearchRequest.getSearchInterval();

                if (endTime>=bgTime){

                    Integer bgDayTimestamp = getDayTimeStamp(bgTime);
                    Integer endDayTimestamp = getDayTimeStamp(endTime);

                    List<TagPointList> tagPointLists = new ArrayList<>();
                    for (String tag : tagList) {

                        byte[] startRowkey = rowKeyUtil.tag2Rowkey(tag, bgDayTimestamp);
                        byte[] endRowkey = rowKeyUtil.tag2Rowkey(tag,endDayTimestamp);

                        TagPointList tagPointList = new TagPointList();
                        tagPointLists.add(tagPointList);
                        tagPointList.setTag(tag);

                        List<SdtPeriod> mergedSdtPeriodList = new ArrayList<>();

                        try {
                            ResultScanner resultScanner = hBaseUtil.scanByStartAndStopRowKey(hbaseTableName,startRowkey,endRowkey);

                            Iterator<Result> iterator = resultScanner.iterator();
                            while (iterator.hasNext()) {

                                Result myresult = iterator.next();
                                Assert.assertNotNull(myresult);
                                byte[] rowKey = myresult.getRow();
                                FlashRowkey flashRowkey = rowKeyUtil.rowkey2FlashRowkey(rowKey);
                                System.out.println(flashRowkey);

                                for (int i =0;i<DAY_HOURS;i++){
                                    List<SdtPeriod> sdtPeriodList = JSON.parseArray(hBaseUtil.getValueByResult(myresult,defaultColumnFamily,i+""),SdtPeriod.class);
                                    if (null!=sdtPeriodList && sdtPeriodList.size()>0) {
                                        mergedSdtPeriodList.addAll(sdtPeriodList);
                                    }
                                }

                            }

                            // uncompress point data
                            if (searchMode.intValue() == PointsSearchMode.INTERPOLATED.getMode().intValue()) {
                                tagPointList.setPointList(sdtService.sdtUnCompress(mergedSdtPeriodList, bgTime, endTime, searchInterval.longValue() * 1000L));
                            }else {
                                // todo

                            }

                        } catch (Exception e) {
                            logger.error("Hbase scan data error:{}",e.getMessage(),e);
                            e.printStackTrace();
                        }

                    }
                    return tagPointLists;

                }


            }

        }

        return new ArrayList<>();
    }


}
