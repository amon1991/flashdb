package com.amon.flashtsdb.sdt;

import com.alibaba.fastjson.JSON;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yaming.chen@foxmail.com
 * Created by chenyaming on 2019/11/27.
 */
@SpringBootTest
public class SdtServiceTest {

    @Autowired
    private SdtService sdtService;

    @Test
    void sdtCompress() throws IOException, ParseException {

        List<Point> pointList = getPointsFromTestFile();

        List<SdtPeriod> sdtPeriodList = sdtService.sdtCompress(pointList, 1d);

        System.out.println(JSON.toJSONString(sdtPeriodList));

        Assert.assertEquals(4, sdtPeriodList.size());

        sdtPeriodList = sdtService.sdtCompress(pointList, 0.2d);
        Assert.assertEquals(11, sdtPeriodList.size());

    }

    @Test
    void sdtUnCompress() throws IOException, ParseException {

        List<Point> pointList = getPointsFromTestFile();
        List<SdtPeriod> sdtPeriodList = sdtService.sdtCompress(pointList, 1d);

        long bgTime = pointList.get(0).getX();
        long endTime = pointList.get(pointList.size() - 1).getX();

        List<Point> unCompressPointList = sdtService.sdtUnCompress(sdtPeriodList, bgTime, endTime, 60 * 1000L);

        Assert.assertEquals(30, unCompressPointList.size());


        unCompressPointList = sdtService.sdtUnCompress(sdtPeriodList, bgTime - 550 * 1000L, endTime - 550 * 1000L, 60 * 1000L);

        Assert.assertEquals(20, unCompressPointList.size());

        unCompressPointList = sdtService.sdtUnCompress(sdtPeriodList, bgTime + 550 * 1000L, endTime + 550 * 1000L, 60 * 1000L);

        Assert.assertEquals(20, unCompressPointList.size());

        unCompressPointList = sdtService.sdtUnCompress(sdtPeriodList, bgTime - 550 * 1000L, endTime + 550 * 1000L, 60 * 1000L);
        Assert.assertEquals(29, unCompressPointList.size());

        unCompressPointList = sdtService.sdtUnCompress(sdtPeriodList, bgTime - 5500 * 1000L, endTime - 5500 * 1000L, 60 * 1000L);
        Assert.assertEquals(0, unCompressPointList.size());

        unCompressPointList = sdtService.sdtUnCompress(sdtPeriodList, bgTime + 5500 * 1000L, endTime + 5500 * 1000L, 60 * 1000L);
        Assert.assertEquals(0, unCompressPointList.size());


    }

    public List<Point> getPointsFromTestFile() throws IOException, ParseException {
        File file = ResourceUtils.getFile("classpath:sdt/testdata/test_data.txt");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        List<String> dataList = new ArrayList<>();
        if (file.isFile() && file.exists()) {

            InputStreamReader read = new InputStreamReader(new FileInputStream(file));

            BufferedReader bufferedReader = new BufferedReader(read);
            String lineTxt;
            while ((lineTxt = bufferedReader.readLine()) != null) {
                dataList.add(lineTxt);
            }
            bufferedReader.close();

        }

        List<Point> pointList = new ArrayList<>();
        if (null != dataList && dataList.size() > 0) {

            for (int i = 0, size = dataList.size(); i < size; i++) {
                if (i != 0) {
                    String data = dataList.get(i);
                    String[] dataArray = data.split("\t");
                    Point point = new Point();
                    point.setX(sdf.parse(dataArray[0]).getTime());
                    point.setY(Double.parseDouble(dataArray[1]));
                    pointList.add(point);
                }
            }

        }
        return pointList;
    }


}