package com.amon.flashtsdb.service.impl;

import com.alibaba.fastjson.JSON;
import com.amon.flashtsdb.entity.*;
import com.amon.flashtsdb.hbase.HBaseUtil;
import com.amon.flashtsdb.hbase.RowKeyUtil;
import com.amon.flashtsdb.sdt.Point;
import com.amon.flashtsdb.sdt.SdtService;
import com.amon.flashtsdb.service.FlashDbService;
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
                splitTagPointList.setTag(tagPointList.getTag());

                List<DayData> dayDataList = new ArrayList<>();
                splitTagPointList.setDayDataList(dayDataList);

                // todo: get accuracyE in redis

                // split to day data
                for (Point point : pointList) {


                }

            }

            return splitTagPointLists;

        }
        return new ArrayList<>();
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
    public List<Point> searchPoints() {

        return null;
    }

}
