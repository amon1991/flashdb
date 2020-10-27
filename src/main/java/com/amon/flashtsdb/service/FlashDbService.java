package com.amon.flashtsdb.service;

import com.amon.flashtsdb.entity.PointsSearchRequest;
import com.amon.flashtsdb.entity.TagInfo;
import com.amon.flashtsdb.entity.TagPointList;

import java.util.List;
import java.util.Set;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2020/1/20.
 */
public interface FlashDbService {


    /**
     * save data points to flash-tsdb
     *
     * @param tagPointLists
     * @return
     */
    int saveDataPoints(List<TagPointList> tagPointLists, Integer savingMode);


    /**
     * search datapoints by params
     *
     * @param pointsSearchRequest
     * @return
     */
    List<TagPointList> searchPoints(PointsSearchRequest pointsSearchRequest);

    /**
     * create tags in flash-tsdb
     *
     * @param tagInfoList
     * @return
     */
    int createTagList(List<TagInfo> tagInfoList);


    /**
     * delete tags in flash-tsdb
     *
     * @param tagCodeList
     * @return
     */
    Long deleteTagList(List<String> tagCodeList);


    /**
     * check if all tags have been created in the list
     *
     * @param tagCodeSet
     * @return
     */
    boolean checkTagList(Set<String> tagCodeSet);

    /**
     * search tags by regex from flash tsdb
     *
     * @param regex
     * @return
     */
    List<TagInfo> searchTags(String regex, int limit);


}
