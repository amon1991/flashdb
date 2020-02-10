package com.amon.flashtsdb.service;

import com.alibaba.fastjson.JSON;
import com.amon.flashtsdb.entity.*;
import com.amon.flashtsdb.hbase.HBaseUtil;
import com.amon.flashtsdb.hbase.RowKeyUtil;
import com.amon.flashtsdb.sdt.Point;
import com.amon.flashtsdb.sdt.SdtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2020/1/20.
 */
public interface FlashDbService {


    /**
     * convert tagPointLists to SplitTagPointLists
     *
     * @param tagPointLists
     * @return
     */
    List<SplitTagPointList> convert2SplitTagPointList(List<TagPointList> tagPointLists);


    /**
     * dump data to hbase
     *
     * @param dataList
     * @return
     */
    int dump2Hbase(@NotNull List<SplitTagPointList> dataList);


    List<Point> searchPoints();

}
