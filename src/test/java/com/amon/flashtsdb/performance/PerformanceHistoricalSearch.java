package com.amon.flashtsdb.performance;

import com.amon.flashtsdb.entity.PointsSearchRequest;
import com.amon.flashtsdb.entity.TagPointList;
import com.amon.flashtsdb.service.impl.FlashDbServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author yaming.chen@foxmail.com
 * Created by chenyaming on 2020/12/10.
 */
public class PerformanceHistoricalSearch implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private CountDownLatch cdl;

    private FlashDbServiceImpl flashDbService;

    private List<Long> executeTimeList;

    private PointsSearchRequest pointsSearchRequest;

    public PerformanceHistoricalSearch(CountDownLatch cdl,
                                       FlashDbServiceImpl flashDbService,
                                       List<Long> executeTimeList,
                                       PointsSearchRequest pointsSearchRequest) {
        this.cdl = cdl;
        this.flashDbService = flashDbService;
        this.executeTimeList = executeTimeList;
        this.pointsSearchRequest = pointsSearchRequest;
    }

    @Override
    public void run() {

        try {

            long eBgTime = System.currentTimeMillis();
            List<TagPointList> tagPointLists = flashDbService.searchPoints(pointsSearchRequest);
            long eEndTime = System.currentTimeMillis();
            executeTimeList.add(new Long(eEndTime - eBgTime));
            logger.info("search task done, thread name:{}, result size:{}",
                    Thread.currentThread().getName(),
                    tagPointLists.size());

        } catch (Exception e) {
            logger.error("Performance test error:{}", e.getMessage(), e);
            e.printStackTrace();
        } finally {
            cdl.countDown();
        }

    }

}
