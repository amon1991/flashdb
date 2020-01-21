package com.amon.flashtsdb.hbase;

import com.amon.flashtsdb.entity.FlashCell;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.io.compress.Compression;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2019/11/18.
 */
@Component
public class HBaseUtil {

    /**
     * Connection creation is a heavy-weight operation. Connection implementations are thread-safe,
     * so that the client can create a connection once, and share it with different threads.
     */
    private Connection connection;
    public static final Logger LOGGER = LoggerFactory.getLogger(HBaseUtil.class);

    @Value("${hbase.config.hbase.zookeeper.quorum}")
    private String zookeeperQuorum;

    private final ConfigurableApplicationContext context;

    @Autowired
    public HBaseUtil(ConfigurableApplicationContext context) {
        this.context = context;
    }

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

    /**
     * batch insert rows into hbase
     *
     * @param tableName
     * @param dataMap
     * @throws Exception
     */
    public int batchInsertRows(String tableName, Map<byte[], List<FlashCell>> dataMap) throws Exception {

        Admin admin = connection.getAdmin();
        if (admin.tableExists(TableName.valueOf(tableName))) {

            Table table = connection.getTable(TableName.valueOf(tableName));
            List<Put> list = new ArrayList<>();
            for (Map.Entry<byte[], List<FlashCell>> entry : dataMap.entrySet()) {
                byte[] rowkey = entry.getKey();
                List<FlashCell> rows = entry.getValue();
                if (rows.size() > 0) {
                    Put put = new Put(rowkey);
                    for (FlashCell row : rows) {
                        put.addColumn(Bytes.toBytes(row.getColumnFamily()),
                                Bytes.toBytes(row.getColumn()),
                                row.getTimestamp(),
                                Bytes.toBytes(row.getValue()));
                    }
                    list.add(put);
                }
            }
            int insertnum = 0;
            if (list.size() > 0) {
                table.put(list);
                insertnum = list.size();
            }
            table.close();
            return insertnum;

        } else {
            return 0;
        }

    }

    /**
     * delete rowkey
     *
     * @param tableName
     * @param rowkey
     * @throws Exception
     */
    public void deleteRowKey(String tableName, byte[] rowkey) throws Exception {

        Admin admin = connection.getAdmin();
        if (admin.tableExists(TableName.valueOf(tableName))) {

            Table table = connection.getTable(TableName.valueOf(tableName));
            Delete delete = new Delete(rowkey);
            table.delete(delete);
            table.close();

        }

    }

    /**
     * batch delete rowkeys
     *
     * @param tableName
     * @param rowkeys
     * @throws Exception
     */
    public void batchDeleteRowkeys(String tableName, List<byte[]> rowkeys) throws Exception {

        Admin admin = connection.getAdmin();
        if (admin.tableExists(TableName.valueOf(tableName))) {

            Table table = connection.getTable(TableName.valueOf(tableName));

            List<Delete> list = new ArrayList<Delete>();
            for (byte[] rowkey : rowkeys) {
                Delete del = new Delete(rowkey);
                list.add(del);
            }
            table.delete(list);
            table.close();

        }

    }

    /**
     * get one row from hbase
     *
     * @param tableName
     * @param rowkey
     * @return
     * @throws Exception
     */
    public Result getOneRow(String tableName, byte[] rowkey) throws Exception {
        Admin admin = connection.getAdmin();
        if (admin.tableExists(TableName.valueOf(tableName))) {

            Table table = connection.getTable(TableName.valueOf(tableName));
            Get get = new Get(rowkey);
            Result result = table.get(get);
            table.close();
            return result;

        } else {
            return null;
        }

    }

    /**
     * get results in scope of [startRowKey,stopRowKey]
     *
     * @param tableName
     * @param startRowKey
     * @param stopRowKey
     * @return
     * @throws Exception
     */
    public ResultScanner scanByStartAndStopRowKey(String tableName, byte[] startRowKey, byte[] stopRowKey) throws Exception {

        Admin admin = connection.getAdmin();
        if (admin.tableExists(TableName.valueOf(tableName))) {
            Table table = connection.getTable(TableName.valueOf(tableName));
            Scan scan = new Scan();
            scan.withStartRow(startRowKey);
            scan.withStopRow(stopRowKey);
            ResultScanner rs = table.getScanner(scan);
            table.close();
            return rs;
        } else {
            return null;
        }

    }

    /**
     * get value in a Result by columnFalimy and column
     *
     * @param result
     * @param columnFalimy
     * @param column
     * @return
     */
    public String getValueByResult(Result result, String columnFalimy, String column) {

        String value = null;
        if (null != result.getValue(Bytes.toBytes(columnFalimy), Bytes.toBytes(column))) {
            value = new String(result.getValue(Bytes.toBytes(columnFalimy), Bytes.toBytes(column)));
        }
        return value;

    }

    /**
     * print a recoder
     *
     * @param result
     * @throws Exception
     */
    public void printRecord(Result result) throws Exception {

        for (Cell cell : result.rawCells()) {
            System.out.print("rowkey: " + new String(CellUtil.cloneRow(cell)) + " ");
            System.out.print("family: " + new String(CellUtil.cloneFamily(cell)) + " ");
            System.out.print("column: " + new String(CellUtil.cloneQualifier(cell)) + " ");
            System.out.print("value: " + new String(CellUtil.cloneValue(cell)) + " ");
            System.out.println("timestamp: " + cell.getTimestamp() + " ");
        }

    }


}
