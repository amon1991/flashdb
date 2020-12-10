package com.amon.flashtsdb.performance;

import com.amon.flashtsdb.FlashtsdbApplication;
import com.amon.flashtsdb.entity.PointsSearchMode;
import com.amon.flashtsdb.entity.PointsSearchRequest;
import com.amon.flashtsdb.entity.TagInfo;
import com.amon.flashtsdb.service.impl.FlashDbServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2020/12/9.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlashtsdbApplication.class)
public class SearchTest {


    @Autowired
    private FlashDbServiceImpl flashDbService;

    private int initnum = 1000;

    @Test
    public void rawSearchTest() {

        List<TagInfo> tagInfoList = flashDbService.searchTags("", initnum);

        List<String> tagcodeList = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            tagcodeList.add(tagInfoList.get(i).getTagCode());
        }

        // one day
        long bgTime = 1559347200000L;
        long endTime = 1559433600000L;

        // ten day
        //long endTime = 1560211200000L;

        // one month
        //long endTime = 1561939200000L;

        PointsSearchRequest pointsSearchRequest = new PointsSearchRequest();
        pointsSearchRequest.setBgTime(bgTime);
        pointsSearchRequest.setEndTime(endTime);
        pointsSearchRequest.setSearchInterval(60);
        pointsSearchRequest.setTagList(tagcodeList);
        pointsSearchRequest.setLimit(-1);
        pointsSearchRequest.setSearchMode(PointsSearchMode.RAW.getMode());


        int currentSize = 10;

        CountDownLatch cdl = new CountDownLatch(currentSize);
        List<Long> executeTimeList = Collections.synchronizedList(new ArrayList<>());

        long eBgTime = System.currentTimeMillis();
        for (int i = 0; i < currentSize; i++) {
            Thread thread = new Thread(new PerformanceSearch(cdl, flashDbService, executeTimeList, pointsSearchRequest));
            thread.start();
        }
        try {
            // 等待所有并发线程执行完毕
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long eEndTime = System.currentTimeMillis();
        System.out.println("All time:" + (eEndTime - eBgTime) + "ms");

        long sumTime = 0;
        for (Long time : executeTimeList) {
            sumTime += time;
        }
        System.out.println("Avg time:" + (sumTime / executeTimeList.size()) + "ms");

    }


}
