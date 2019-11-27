package com.amon.flashtsdb.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.io.compress.Compression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2019/11/18.
 */
@Component
public class HBaseAPI {

    /**
     * Connection creation is a heavy-weight operation. Connection implementations are thread-safe,
     * so that the client can create a connection once, and share it with different threads.
     */
    private Connection connection;
    public static final Logger LOGGER = LoggerFactory.getLogger(HBaseAPI.class);

    @Value("${hbase.config.hbase.zookeeper.quorum}")
    private String zookeeperQuorum;

    @Autowired
    private ConfigurableApplicationContext context;

    /**
     * Init hbase connection
     */
    private void initHbaseConnection() {
        if (null == connection) {
            try {
                Configuration configuration = new Configuration();
                configuration.set("hbase.zookeeper.quorum", zookeeperQuorum);
                connection = ConnectionFactory.createConnection(HBaseConfiguration.create(configuration));
            } catch (IOException e) {
                LOGGER.error("Connect to hbase failed,error message:{}", e.getMessage(), e);
                context.close();
            }
        }
    }

    /**
     * create a new table in hbase
     *
     * @param tableName     table name
     * @param columnFamilys column family
     */
    public void createTable(String tableName, List<String> columnFamilys) throws Exception {

        initHbaseConnection();
        Admin admin = connection.getAdmin();
        if (admin.tableExists(TableName.valueOf(tableName))) {
            LOGGER.info("Table exist,skip creation,table name:{}", tableName);
        } else {
            HTableDescriptor tableDesc = new HTableDescriptor(TableName.valueOf(tableName));
            for (String columnFamily : columnFamilys) {
                // set table Compression is gz (save space)
                tableDesc.addFamily(new HColumnDescriptor(columnFamily).setCompactionCompressionType(Compression.Algorithm.SNAPPY));
            }
            admin.createTable(tableDesc);
            LOGGER.info("Create table successfully,table name:{}", tableName);
        }
        admin.close();

    }

    /**
     * delete a table in hbase
     *
     * @param tableName table name
     */
    public void deleteTable(String tableName) throws Exception {
        initHbaseConnection();
        Admin admin = connection.getAdmin();
        if (admin.tableExists(TableName.valueOf(tableName))) {
            admin.disableTable(TableName.valueOf(tableName));
            admin.deleteTable(TableName.valueOf(tableName));
            LOGGER.info("Delete table successfully,table name:{}", tableName);
        } else {
            LOGGER.info("The table don't exist,table name:{}", tableName);
        }
        admin.close();
    }


}
