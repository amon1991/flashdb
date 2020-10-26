package com.amon.flashtsdb.service.impl;

import com.alibaba.fastjson.JSON;
import com.amon.flashtsdb.entity.*;
import com.amon.flashtsdb.hbase.HBaseUtil;
import com.amon.flashtsdb.hbase.RowKeyUtil;
import com.amon.flashtsdb.sdt.Point;
import com.amon.flashtsdb.sdt.SdtPeriod;
import com.amon.flashtsdb.sdt.SdtPoints;
import com.amon.flashtsdb.sdt.SdtService;
import com.amon.flashtsdb.service.FlashDbService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.amon.flashtsdb.hbase.RowKeyUtil.TAGID_INDEX;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2020/1/20.
 */
@Service
public class FlashDbServiceImpl implements FlashDbService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final static int DAY_HOURS = 24;

    public static final String TAGINFO_LIST = "TAGINFO_LIST:";

    @Value("${flashtsdb.config.tablename}")
    private String hbaseTableName;

    @Value("${flashtsdb.config.accuracy}")
    private Double defaultAccuracy;

    private static final String defaultColumnFamily = "t";

    private final HBaseUtil hBaseUtil;
    private final RowKeyUtil rowKeyUtil;
    private final SdtService sdtService;
    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    public FlashDbServiceImpl(HBaseUtil hBaseUtil,
                              RowKeyUtil rowKeyUtil,
                              SdtService sdtService,
                              StringRedisTemplate stringRedisTemplate) {
        this.hBaseUtil = hBaseUtil;
        this.rowKeyUtil = rowKeyUtil;
        this.sdtService = sdtService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public int saveDataPoints(List<TagPointList> tagPointLists) {
        return dump2Hbase(convert2SplitTagPointList(tagPointLists));
    }

    /**
     * convert tagPointLists to SplitTagPointLists
     * （将原始数据转换为hbase存储数据）
     *
     * @param tagPointLists
     * @return
     */
    public List<SplitTagPointList> convert2SplitTagPointList(List<TagPointList> tagPointLists) {

        if (null != tagPointLists && tagPointLists.size() > 0) {

            List<SplitTagPointList> splitTagPointLists = new ArrayList<>();

            Set<String> tagSet = tagPointLists.stream().map(p -> TAGINFO_LIST + p.getTag()).collect(Collectors.toSet());

            Map<String, String> tagMap = redisStringBatchGet(new ArrayList<>(tagSet));
            Map<String, TagInfo> tagInfoMap = new HashMap<>();
            for (String tagInfoStr : tagMap.values()) {
                if (null != tagInfoStr) {
                    TagInfo tagInfo = JSON.parseObject(tagInfoStr, TagInfo.class);
                    tagInfoMap.put(tagInfo.getTagCode(), tagInfo);
                }
            }

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

                if (tagInfoMap.containsKey(tag)) {
                    splitTagPointList.setAccuracyE(tagInfoMap.get(tag).getAccuracyE());
                } else {
                    splitTagPointList.setAccuracyE(defaultAccuracy);
                }

                int startDayTimestamp = 0;
                int startHourQualifier = -1;
                DayData dayData = null;
                HourData hourData = null;

                // split to day data
                for (Point point : pointList) {

                    Integer currnetDayTime = getDayTimeStamp(point.getX());

                    if (currnetDayTime > startDayTimestamp) {
                        startDayTimestamp = currnetDayTime;
                        dayData = new DayData();
                        dayData.setTimestamp(currnetDayTime);
                        dayData.setHourDataList(new ArrayList<>());
                        dayDataList.add(dayData);
                    }

                    Integer currentHourQualifier = getHourQualifier(point.getX());

                    if (currentHourQualifier.intValue() != startHourQualifier) {
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

    public Integer getDayTimeStamp(long timestamp) {
        return Math.toIntExact(timestamp / (24 * 3600 * 1000L));
    }

    public Integer getHourQualifier(long timestamp) {
        return Math.toIntExact(timestamp / (3600 * 1000L)) % 24;
    }


    /**
     * dump data to hbase
     * ps：将数据存储到hbase，rowkey的粒度tag+1day，其中每个rowkey最多有24个Column（每小时一个），每个column中的数据进行sdt压缩
     *
     * @param dataList
     * @return
     */
    private int dump2Hbase(@NotNull List<SplitTagPointList> dataList) {

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

        if (null != pointsSearchRequest && null != pointsSearchRequest.getSearchMode()) {

            if (null != pointsSearchRequest.getTagList() && pointsSearchRequest.getTagList().size() > 0) {

                List<String> tagList = pointsSearchRequest.getTagList();
                Integer searchMode = pointsSearchRequest.getSearchMode();
                long bgTime = pointsSearchRequest.getBgTime();
                long endTime = pointsSearchRequest.getEndTime();
                Integer searchInterval = pointsSearchRequest.getSearchInterval();

                if (endTime >= bgTime) {

                    int limit = pointsSearchRequest.getLimit();
                    boolean limited = false;
                    if (limit > 0) {
                        limited = true;
                        if (pointsSearchRequest.getSearchMode().intValue() == PointsSearchMode.INTERPOLATED.getMode()) {
                            // calculate interval by bgtime\endtime\limit
                            searchInterval = Math.toIntExact((endTime - bgTime) / (limit * 1000L)) + 1;
                        }
                    }

                    Integer bgDayTimestamp = getDayTimeStamp(bgTime);
                    Integer endDayTimestamp = getDayTimeStamp(endTime);

                    Map<String, HourRange> searchScopeMap = new HashMap<>(16);

                    if (bgDayTimestamp.intValue() == endDayTimestamp.intValue()) {

                        HourRange hourRange = new HourRange();
                        hourRange.setBgHour(getHourQualifier(bgTime));
                        hourRange.setEndHour(getHourQualifier(endTime));

                        // sameday
                        searchScopeMap.put(bgDayTimestamp + "", hourRange);

                    } else {
                        // different day
                        for (int i = bgDayTimestamp; i <= endDayTimestamp; i++) {

                            HourRange hourRange = new HourRange();
                            if (i == bgDayTimestamp) {
                                hourRange.setBgHour(getHourQualifier(bgTime));
                                hourRange.setEndHour(23);
                            } else if (i == endDayTimestamp) {
                                hourRange.setBgHour(0);
                                hourRange.setEndHour(getHourQualifier(endTime));
                            } else {
                                hourRange.setBgHour(0);
                                hourRange.setEndHour(23);
                            }
                            searchScopeMap.put(i + "", hourRange);

                        }

                    }


                    List<TagPointList> tagPointLists = new ArrayList<>();

                    // todo: use currnt search to increase performace
                    for (String tag : tagList) {

                        if (endDayTimestamp > bgDayTimestamp) {
                            // hbase scan will not include endDayTimestamp,so the endday should add one
                            endDayTimestamp++;
                        }

                        byte[] startRowkey = rowKeyUtil.tag2Rowkey(tag, bgDayTimestamp);
                        byte[] endRowkey = rowKeyUtil.tag2Rowkey(tag, endDayTimestamp);

                        TagPointList tagPointList = new TagPointList();
                        tagPointLists.add(tagPointList);
                        tagPointList.setTag(tag);

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
                        }

                    }
                    return tagPointLists;

                }


            }

        }

        return new ArrayList<>();
    }

    @Override
    public int createTagList(List<TagInfo> tagInfoList) {

        if (null != tagInfoList && tagInfoList.size() > 0) {

            List<String> keys = tagInfoList.stream().map(tagInfo -> {
                return TAGINFO_LIST + tagInfo.getTagCode();
            }).collect(Collectors.toList());
            Map<String, String> exsitTagMap = redisStringBatchGet(keys);
            Map<String, String> saveMap = new HashMap<>(keys.size() * 2);

            for (TagInfo tagInfo : tagInfoList) {
                if (null != tagInfo && null != tagInfo.getTagCode()) {
                    if (StringUtils.isEmpty(exsitTagMap.get(TAGINFO_LIST + tagInfo.getTagCode()))) {
                        saveMap.put(TAGINFO_LIST + tagInfo.getTagCode(), JSON.toJSONString(tagInfo));
                    }
                }
            }

            // batch insert data to redis
            redisStringBatchInsert(saveMap);
            return saveMap.size();

        }

        return 0;
    }

    @Override
    public Long deleteTagList(List<String> tagCodeList) {

        if (null != tagCodeList && tagCodeList.size() > 0) {

            List<String> tagInfoKeys = tagCodeList.stream().map(tagcode -> {
                return TAGINFO_LIST + tagcode;
            }).collect(Collectors.toList());

            List<String> tagIndexKeys = tagCodeList.stream().map(tagcode -> {
                return TAGID_INDEX + tagcode;
            }).collect(Collectors.toList());

            Map<String, String> tagInfoTagMap = redisStringBatchGet(tagInfoKeys);

            Map<String, String> tagIndexTagMap = redisStringBatchGet(tagIndexKeys);

            List<String> deleteList = new ArrayList<>();

            for (String tagCode : tagCodeList) {

                if (StringUtils.isNotEmpty(tagInfoTagMap.get(TAGINFO_LIST + tagCode))
                        && StringUtils.isEmpty(tagIndexTagMap.get(TAGID_INDEX + tagCode))) {
                    // the tag exsist in taginfoRedis but not exsist in indexRedis
                    deleteList.add(TAGINFO_LIST + tagCode);
                }

            }

            return this.redisStringBatchDelete(deleteList);

        }

        return 0L;
    }

    /**
     * check if all tags have been created in the list
     *
     * @param tagCodeSet
     * @return
     */
    @Override
    public boolean checkTagList(Set<String> tagCodeSet) {

        if (CollectionUtils.isNotEmpty(tagCodeSet)) {

            List<String> keys = tagCodeSet.stream().map(tagCode -> {
                return TAGINFO_LIST + tagCode;
            }).collect(Collectors.toList());
            Map<String, String> exsitTagMap = redisStringBatchGet(keys);

            int exsistSize = 0;
            for (String value : exsitTagMap.values()) {
                if (null != value) {
                    exsistSize++;
                }
            }

            if (tagCodeSet.size() == exsistSize) {
                return true;
            }

        }

        return false;
    }

    /**
     * search tags by regex from flash tsdb
     *
     * @param regex
     * @param limit
     * @return
     */
    @Override
    public List<TagInfo> searchTags(String regex, int limit) {

        //Set<String> keyList = stringRedisTemplate.keys(TAGINFO_LIST + "*" + regex + "*");

        Set<String> keyList = new HashSet<>(16 * 16);

        ScanOptions scanOptions = ScanOptions.scanOptions().match(TAGINFO_LIST + "*" + regex + "*").count(limit).build();
        RedisSerializer<String> redisSerializer = (RedisSerializer<String>) stringRedisTemplate.getKeySerializer();
        Cursor<String> cursor = (Cursor) stringRedisTemplate.executeWithStickyConnection((RedisCallback) redisConnection ->
                new ConvertingCursor<>(redisConnection.scan(scanOptions), redisSerializer::deserialize));
        while (cursor.hasNext() && limit > keyList.size()) {
            keyList.add(cursor.next());
        }

        try {
            cursor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, String> tagMap = redisStringBatchGet(new ArrayList<>(keyList));

        List<TagInfo> tagInfoList = new ArrayList<>();

        for (String tagInfo : tagMap.values()) {
            tagInfoList.add(JSON.parseObject(tagInfo, TagInfo.class));
        }

        return tagInfoList;
    }

    /**
     * batch get string data from redis
     *
     * @param keyList
     * @return
     */
    private Map<String, String> redisStringBatchGet(List<String> keyList) {

        if (null != keyList && keyList.size() > 0) {

            List<Object> objects = stringRedisTemplate.executePipelined((RedisCallback<String>) redisConnection -> {
                StringRedisConnection stringRedisConnection = (StringRedisConnection) redisConnection;
                for (String key : keyList) {
                    stringRedisConnection.get(key);
                }
                return null;
            });

            List<String> collect = objects.stream().map(val -> String.valueOf(val)).collect(Collectors.toList());
            Map<String, String> map = new HashMap<>(collect.size() * 2);

            for (int i = 0, size = keyList.size(); i < size; i++) {
                map.put(keyList.get(i), collect.get(i).equals("null") ? null : collect.get(i));
            }

            return map;

        }

        return new HashMap<>(0);

    }

    /**
     * batch delete
     *
     * @param keyList
     * @return
     */
    private Long redisStringBatchDelete(List<String> keyList) {

        if (null != keyList && keyList.size() > 0) {

            return stringRedisTemplate.delete(keyList);

        }

        return 0L;

    }

    /**
     * batch insert string data into redis
     *
     * @param saveMap
     */
    private void redisStringBatchInsert(Map<String, String> saveMap) {

        stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> redisOperations) throws DataAccessException {

                for (Map.Entry<String, String> entry : saveMap.entrySet()) {
                    stringRedisTemplate.opsForValue().set(entry.getKey(), entry.getValue());
                }

                return null;
            }
        });

    }


}
