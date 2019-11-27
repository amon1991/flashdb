package com.amon.flashtsdb.hbase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2019/11/19.
 */
@SpringBootTest
class HBaseAPITest {

    @Autowired
    private HBaseAPI hBaseAPI;


    @Test
    void createTable() {

        try {
            hBaseAPI.createTable("flashtsdb", Arrays.asList("t"));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    void deleteTable() {

        try {
            hBaseAPI.deleteTable("flashtsdb");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}