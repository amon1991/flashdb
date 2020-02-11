package com.amon.flashtsdb.service.impl;

import com.amon.flashtsdb.FlashtsdbApplication;
import com.amon.flashtsdb.entity.SplitTagPointList;
import com.amon.flashtsdb.entity.TagPointList;
import com.amon.flashtsdb.sdt.Point;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

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
        long skipTime = 1800*1000L;
        for (int i = 0; i < 100; i++) {
            Point point = new Point();
            point.setX(startTime);
            point.setY(10.0d);
            tagPointList.getPointList().add(point);
            startTime+=skipTime;
        }

        List<SplitTagPointList> splitTagPointLists = flashDbService.convert2SplitTagPointList(tagPointLists);
        Assert.assertNotNull(splitTagPointLists);
        Assert.assertEquals(3,splitTagPointLists.get(0).getDayDataList().size());

    }
}
