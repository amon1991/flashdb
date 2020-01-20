package com.amon.flashtsdb.hbase;

import com.amon.flashtsdb.FlashtsdbApplication;
import com.amon.flashtsdb.entity.FlashCell;
import com.amon.flashtsdb.entity.FlashRowkey;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2019/11/19.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlashtsdbApplication.class)
public class HBaseAPITest {


    private static final String testTableName = "flashtsdbtest";
    private static final String defaultColumnFamily = "t";
    private static final String columnValue01 = "testValue01";
    private static final String columnValue02 = "testValue02";

    @Autowired
    private HBaseUtil hbaseUtil;

    @Autowired
    private RowKeyUtil rowKeyUtil;


    @Test
    public void createTableAndDeleteTable() {

        try {
            hbaseUtil.createTable(testTableName, Arrays.asList(defaultColumnFamily));
            hbaseUtil.deleteTable(testTableName);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void deleteTable() {

        try {
            hbaseUtil.deleteTable(testTableName);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void insertAndDeleteTable() {

        try {
            hbaseUtil.createTable(testTableName, Arrays.asList(defaultColumnFamily));

            String tag = "@10MAC11CP001_XQ01";
            Integer timestamp = Math.toIntExact(System.currentTimeMillis() / (24 * 3600 * 1000L));
            System.out.println("timestamp:" + timestamp);
            byte[] rowKey01 = rowKeyUtil.tag2Rowkey(tag, timestamp);
            byte[] rowKey02 = rowKeyUtil.tag2Rowkey(tag, timestamp + 1);
            byte[] rowKey03 = rowKeyUtil.tag2Rowkey(tag, timestamp + 2);

            List<FlashCell> flashRows = new ArrayList<>();
            FlashCell flashRow = new FlashCell();
            flashRow.setColumnFamily(defaultColumnFamily);
            flashRow.setColumn("1");
            flashRow.setValue(columnValue01);
            flashRows.add(flashRow);
            FlashCell flashRow02 = new FlashCell();
            flashRow02.setColumnFamily(defaultColumnFamily);
            flashRow02.setColumn("2");
            flashRow02.setValue(columnValue02);
            flashRows.add(flashRow02);

            Map<byte[], List<FlashCell>> dataMap = new HashMap<>();
            dataMap.put(rowKey01, flashRows);
            dataMap.put(rowKey02, flashRows);

            // test batch insert
            hbaseUtil.batchInsertRows(testTableName, dataMap);

            // test get one row
            Result result = hbaseUtil.getOneRow(testTableName, rowKey01);
            Assert.assertNotNull(result);
            Assert.assertEquals(columnValue01, hbaseUtil.getValueByResult(result, defaultColumnFamily, "1"));

            // test get results in a scope
            ResultScanner resultScanner = hbaseUtil.scanByStartAndStopRowKey(testTableName, rowKey01, rowKey03);
            Assert.assertNotNull(resultScanner);

            Iterator<Result> iterator = resultScanner.iterator();
            int index = 0;
            while (iterator.hasNext()) {

                Result myresult = iterator.next();
                Assert.assertNotNull(myresult);
                byte[] rowKey = myresult.getRow();
                FlashRowkey flashRowkey = rowKeyUtil.rowkey2FlashRowkey(rowKey);
                System.out.println(flashRowkey);

                Assert.assertEquals(columnValue01, hbaseUtil.getValueByResult(result, defaultColumnFamily, "1"));
                Assert.assertEquals(columnValue02, hbaseUtil.getValueByResult(result, defaultColumnFamily, "2"));
                index++;

            }

            Assert.assertEquals(2, index);

            // test batch rows delete
            List<byte[]> rowlkeys = new ArrayList<>();
            rowlkeys.add(rowKey01);
            rowlkeys.add(rowKey02);

            hbaseUtil.batchDeleteRowkeys(testTableName, rowlkeys);

            resultScanner = hbaseUtil.scanByStartAndStopRowKey(testTableName, rowKey01, rowKey03);
            Assert.assertEquals(false, resultScanner.iterator().hasNext());

            hbaseUtil.deleteTable(testTableName);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }


}