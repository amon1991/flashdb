package com.amon.flashtsdb.performance;

import com.alibaba.fastjson.JSON;
import com.amon.flashtsdb.FlashtsdbApplication;
import com.amon.flashtsdb.entity.PointsSavingMode;
import com.amon.flashtsdb.entity.TagInfo;
import com.amon.flashtsdb.entity.TagPointList;
import com.amon.flashtsdb.sdt.Point;
import com.amon.flashtsdb.service.impl.FlashDbServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2020/10/30.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlashtsdbApplication.class)
public class InsertTest {

    @Autowired
    private FlashDbServiceImpl flashDbService;

    private int initnum = 1000;

    @Test
    public void testInsert() {

        List<TagInfo> tagInfoList = flashDbService.searchTags("", initnum);

        List<String> tagcodeList = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            tagcodeList.add(tagInfoList.get(i).getTagCode());
        }

        List<TagPointList> tagPointLists = new ArrayList<>();
        List<Point> pointList = new ArrayList<>();

        int begin = 0;

        System.out.println(JSON.toJSONString(tagcodeList));

        long bgTime = System.currentTimeMillis();
        int successnum = doBatchInsert(tagcodeList, tagPointLists, pointList, begin);
        long endTime = System.currentTimeMillis();
        System.out.println("batch use time:" + (endTime - bgTime) + "ms,success num:" + successnum);

    }

    private int doBatchInsert(List<String> tagcodeList, List<TagPointList> tagPointLists, List<Point> pointList, int begin) {


        long insertBgtime = 1546300800000L;
        long insertEndtime = 1577836800000L;

        //Random random = new Random();
        while (insertEndtime > insertBgtime) {

            Point point = new Point();
            point.setX(insertBgtime);
            point.setY(100 * Math.sin(begin++));
            pointList.add(point);
            insertBgtime += 60 * 1000L;

        }


        for (String tagcode : tagcodeList) {

            TagPointList tagPointList = new TagPointList();
            tagPointList.setTag(tagcode);
            tagPointList.setPointList(pointList);
            tagPointLists.add(tagPointList);

        }

        //System.out.println();
        return flashDbService.saveDataPoints(tagPointLists, PointsSavingMode.COVER.getMode());
    }

    @Test
    public void createTags() {

        long bgTime = System.currentTimeMillis();
        // 1w
        createTags(initnum);
        long endTime = System.currentTimeMillis();
        System.out.println("create tag num: " + initnum + " Use time:" + (endTime - bgTime) + "ms");

    }

    private void createTags(int initnum) {

        List<TagInfo> tagInfoList = new ArrayList<>();

        for (int i = 0; i < initnum; i++) {
            tagInfoList.add(new TagInfo("tagcode" + UUID.randomUUID().toString(),
                    "tagName", "tagDescription", "ka", 20, System.currentTimeMillis()));
        }

        int successNum = flashDbService.createTagList(tagInfoList);
        Assert.assertEquals(initnum, successNum);

    }


}
