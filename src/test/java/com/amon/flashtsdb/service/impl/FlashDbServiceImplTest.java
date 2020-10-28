package com.amon.flashtsdb.service.impl;

import com.alibaba.fastjson.JSON;
import com.amon.flashtsdb.FlashtsdbApplication;
import com.amon.flashtsdb.entity.*;
import com.amon.flashtsdb.sdt.Point;
import com.amon.flashtsdb.sdt.SdtServiceTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2020/2/11.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlashtsdbApplication.class)
public class FlashDbServiceImplTest {

    @Autowired
    private FlashDbServiceImpl flashDbService;

    @Test
    public void testRealtimeSetAndGet() {

        String tagCode01 = "tagCode001";
        String tagCode02 = "tagCode002";
        Random random = new Random();

        Map<String, Point> pointMap = new HashMap<>();
        int value1 = random.nextInt(100);
        int value2 = random.nextInt(100);

        pointMap.put(tagCode01, new Point(System.currentTimeMillis(), value1));
        pointMap.put(tagCode02, new Point(System.currentTimeMillis(), value2));
        flashDbService.saveRealtimePoints(pointMap);

        Map<String, Point> dbPointMap = flashDbService.searchRealtimePoints(new HashSet<>(Arrays.asList(tagCode01, tagCode02)));
        Assert.assertEquals((int) dbPointMap.get(tagCode01).getY(), value1);
        Assert.assertEquals((int) dbPointMap.get(tagCode02).getY(), value2);

    }

    @Test
    public void getDayTimeStamp() throws ParseException {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timeStr = "2020-01-20 12:11:10";
        long timestamp = sdf.parse(timeStr).getTime();

        Integer dayTime = flashDbService.getDayTimeStamp(timestamp);
        Assert.assertNotNull(dayTime);

        Integer hourQualifier = flashDbService.getHourQualifier(timestamp);
        Assert.assertEquals(12, hourQualifier.intValue());

        timeStr = "2020-01-20 0:0:0";
        timestamp = sdf.parse(timeStr).getTime();
        hourQualifier = flashDbService.getHourQualifier(timestamp);
        Assert.assertEquals(0, hourQualifier.intValue());

        timeStr = "2020-01-20 1:0:0";
        timestamp = sdf.parse(timeStr).getTime();
        hourQualifier = flashDbService.getHourQualifier(timestamp);
        Assert.assertEquals(1, hourQualifier.intValue());

        timeStr = "2020-01-20 23:59:59";
        timestamp = sdf.parse(timeStr).getTime();
        hourQualifier = flashDbService.getHourQualifier(timestamp);
        Assert.assertEquals(23, hourQualifier.intValue());

    }

    @Test
    public void convert2SplitTagPointList() throws ParseException {

        List<TagPointList> tagPointLists = new ArrayList<>();

        TagPointList tagPointList = new TagPointList();
        tagPointList.setTag("Tag01");
        tagPointList.setPointList(new ArrayList<>());
        tagPointLists.add(tagPointList);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        long startTime = sdf.parse("2020-01-20 12:11:10").getTime();
        long skipTime = 1800 * 1000L;
        for (int i = 0; i < 100; i++) {
            Point point = new Point();
            point.setX(startTime);
            point.setY(10.0d);
            tagPointList.getPointList().add(point);
            startTime += skipTime;
        }

        List<SplitTagPointList> splitTagPointLists =
                flashDbService.convert2SplitTagPointList(tagPointLists, PointsSavingMode.MERGE.getMode());
        Assert.assertNotNull(splitTagPointLists);
        Assert.assertEquals(3, splitTagPointLists.get(0).getDayDataList().size());

    }

    @Test
    public void insertLongTimeData() {

        List<TagPointList> tagPointLists = new ArrayList<>();

        String[] tagCodeArray = {"tagCode001", "tagCode002", "tagCode003"};

        List<Point> pointList = new ArrayList<>();

        long bgTimeStamp = 1603843200000L;
        long currentTimeStamp = System.currentTimeMillis();

        Random random = new Random();
        while (currentTimeStamp > bgTimeStamp) {

            Point point = new Point();
            point.setX(bgTimeStamp);
            point.setY(random.nextInt(1000));
            pointList.add(point);
            bgTimeStamp += 60 * 1000L;

        }

        for (String tagCode : tagCodeArray) {

            TagPointList tagPointList = new TagPointList();
            tagPointList.setTag(tagCode);
            tagPointList.setPointList(pointList);
            tagPointLists.add(tagPointList);

        }

        //System.out.println();
        int successnum = flashDbService.saveDataPoints(tagPointLists, PointsSavingMode.COVER.getMode());
        Assert.assertNotEquals(0, successnum);

    }

    @Test
    public void saveDataPointsAndSearchPoints() throws IOException, ParseException {

        String tag = "TestTag01";

        List<Point> pointList = new SdtServiceTest().getPointsFromTestFile();

        List<TagPointList> tagPointLists = new ArrayList<>();
        TagPointList tagPointList = new TagPointList();
        tagPointList.setTag(tag);
        tagPointList.setPointList(pointList);
        tagPointLists.add(tagPointList);

        System.out.println(JSON.toJSONString(tagPointLists));

        int successnum = flashDbService.saveDataPoints(tagPointLists, PointsSavingMode.COVER.getMode());
        Assert.assertNotEquals(0, successnum);

        long bgTime = pointList.get(0).getX();
        long endTime = pointList.get(pointList.size() - 1).getX();
        List<String> tagList = Arrays.asList(tag);

        searchPoints(bgTime, endTime, tagList);

    }

    private void searchPoints(long bgTime, long endTime, List<String> tagList) {

        List<TagPointList> tagPointLists;
        PointsSearchRequest pointsSearchRequest = new PointsSearchRequest();
        pointsSearchRequest.setBgTime(bgTime);
        pointsSearchRequest.setEndTime(endTime);
        pointsSearchRequest.setSearchInterval(60);
        pointsSearchRequest.setTagList(tagList);
        pointsSearchRequest.setLimit(1000);
        pointsSearchRequest.setSearchMode(PointsSearchMode.INTERPOLATED.getMode());


        System.out.println(JSON.toJSONString(pointsSearchRequest));
        tagPointLists = flashDbService.searchPoints(pointsSearchRequest);
        Assert.assertNotNull(tagPointLists);

        pointsSearchRequest.setSearchMode(PointsSearchMode.RAW.getMode());

        System.out.println(JSON.toJSONString(pointsSearchRequest));
        tagPointLists = flashDbService.searchPoints(pointsSearchRequest);
        Assert.assertNotNull(tagPointLists);

    }

    @Test
    public void createTagList() {

        List<TagInfo> tagInfoList = new ArrayList<>();

        String tagCode01 = "tagCode1:" + UUID.randomUUID().toString();
        String tagCode02 = "tagCode2:" + UUID.randomUUID().toString();

        TagInfo tagInfo01 = new TagInfo(tagCode01, "tagName", "tagDescription", "tagUnit", 0.1d, System.currentTimeMillis());
        TagInfo tagInfo02 = new TagInfo(tagCode02, "tagName", "tagDescription", "tagUnit", 0.1d, System.currentTimeMillis());

        tagInfoList.add(tagInfo01);
        tagInfoList.add(tagInfo02);

        int successNum = flashDbService.createTagList(tagInfoList);
        Assert.assertEquals(2, successNum);

        String tagCode03 = "tagCode3:" + UUID.randomUUID().toString();
        TagInfo tagInfo03 = new TagInfo(tagCode03, "tagName", "tagDescription", "tagUnit", 0.1d, System.currentTimeMillis());
        tagInfoList.add(tagInfo03);
        successNum = flashDbService.createTagList(tagInfoList);
        Assert.assertEquals(1, successNum);

        successNum = flashDbService.createTagList(tagInfoList);
        Assert.assertEquals(0, successNum);

        List<String> tagCodeList = tagInfoList.stream().map(tagInfo -> {
            return tagInfo.getTagCode();
        }).collect(Collectors.toList());
        int deleteNum = Math.toIntExact(flashDbService.deleteTagList(tagCodeList));
        Assert.assertEquals(3, deleteNum);

    }

}
