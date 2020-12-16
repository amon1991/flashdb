package com.amon.flashtsdb.reboot;

import com.amon.flashtsdb.hbase.HBaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @author yaming.chen@foxmail.com
 * Created by chenyaming on 2020/2/12.
 */
@Component
public class RebootTask implements ApplicationListener<ContextRefreshedEvent> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${flashtsdb.config.tablename}")
    private String hbaseTableName;

    private static final String defaultColumnFamily = "t";

    private final HBaseUtil hBaseUtil;

    public RebootTask(HBaseUtil hBaseUtil) {
        this.hBaseUtil = hBaseUtil;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        // check if the habse table has created，if not, create habse table
        try {
            hBaseUtil.createTable(hbaseTableName, Arrays.asList(defaultColumnFamily));
        } catch (Exception e) {
            logger.error("RebootTask failed,error msg:{}",e.getMessage(),e);
            e.printStackTrace();
        }

    }

}
