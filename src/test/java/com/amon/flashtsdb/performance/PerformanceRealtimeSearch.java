package com.amon.flashtsdb.performance;

import com.amon.flashtsdb.sdt.Point;
import com.amon.flashtsdb.service.impl.FlashDbServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * @author yaming.chen@foxmail.com
 * Created by chenyaming on 2020/12/10.
 */
public class PerformanceRealtimeSearch implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private CountDownLatch cdl;

    private FlashDbServiceImpl flashDbService;

    private List<Long> executeTimeList;

    private Set<String> tagcodeSet;

    public PerformanceRealtimeSearch(CountDownLatch cdl, FlashDbServiceImpl flashDbService, List<Long> executeTimeList, Set<String> tagcodeSet) {
        this.cdl = cdl;
        this.flashDbService = flashDbService;
        this.executeTimeList = executeTimeList;
        this.tagcodeSet = tagcodeSet;
    }

    @Override
    public void run() {

        try {

            long eBgTime = System.currentTimeMillis();
            Map<String, Point> resultMap = flashDbService.searchRealtimePoints(tagcodeSet);
            long eEndTime = System.currentTimeMillis();
            executeTimeList.add(new Long(eEndTime - eBgTime));
            logger.info("search task done, thread name:{}, result size:{}",
                    Thread.currentThread().getName(),
                    resultMap.size());

        } catch (Exception e) {
            logger.error("Performance test error:{}", e.getMessage(), e);
            e.printStackTrace();
        } finally {
            cdl.countDown();
        }
    }

}
