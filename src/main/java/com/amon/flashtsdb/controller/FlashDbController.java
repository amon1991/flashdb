package com.amon.flashtsdb.controller;

import com.amon.flashtsdb.entity.PointsSearchMode;
import com.amon.flashtsdb.entity.PointsSearchRequest;
import com.amon.flashtsdb.entity.TagInfo;
import com.amon.flashtsdb.entity.TagPointList;
import com.amon.flashtsdb.service.FlashDbService;
import com.amon.flashtsdb.service.impl.FlashDbServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2020/10/19.
 */
@Api(value = "flashtsdb restful apis")
@Controller
@RequestMapping("/apis/flashtsdb")
public class FlashDbController {

    private static final String SUCCESS = "success";
    private static final String MSG = "msg";
    private static final String DATA = "data";
    private static final int DEFAULT_TAGS_LIMIT = 1000;

    private final FlashDbService flashDbService;

    public FlashDbController(FlashDbServiceImpl flashDbService) {
        this.flashDbService = flashDbService;
    }


    @ApiOperation(value = "create tags", notes = "create tags")
    @PostMapping(value = "/tags")
    @ResponseBody
    public ModelMap createtags(@RequestBody List<TagInfo> tagInfoList) {
        ModelMap modelMap = new ModelMap();

        if (CollectionUtils.isNotEmpty(tagInfoList)) {


            for (TagInfo tagInfo : tagInfoList) {
                if (tagInfo.getAccuracyE() >= 1 || tagInfo.getAccuracyE() <= 0) {
                    modelMap.put(SUCCESS, false);
                    modelMap.put(MSG, "tag code:" + tagInfo.getTagCode() + ",accuracyE should in (0,1),plesse check.");
                    return modelMap;
                }
                tagInfo.setCreatetime(System.currentTimeMillis());
            }

            int tobeSavedSize = tagInfoList.size();
            int savedSize = flashDbService.createTagList(tagInfoList);

            if (tobeSavedSize != savedSize) {
                modelMap.put(SUCCESS, false);
                modelMap.put(MSG, "tag list size:" + tobeSavedSize + " / created tag size:" + savedSize);
            } else {
                modelMap.put(SUCCESS, true);
                modelMap.put(MSG, "create tags successfully");
            }

        } else {
            modelMap.put(SUCCESS, false);
            modelMap.put(MSG, "tag list can't be null");
        }

        modelMap.put(SUCCESS, true);
        return modelMap;
    }

    @ApiOperation(value = "select all tags", notes = "select all tags")
    @GetMapping(value = "/tags")
    @ResponseBody
    public ModelMap selectAllTags() {

        ModelMap modelMap = new ModelMap();
        modelMap.put(SUCCESS, true);
        modelMap.put(MSG, "get tags successfully");
        modelMap.put(DATA, flashDbService.searchTags("", DEFAULT_TAGS_LIMIT));
        return modelMap;

    }

    @ApiOperation(value = "select tags by regex", notes = "select tags by regex")
    @GetMapping(value = "/tags/{regex}")
    @ResponseBody
    public ModelMap selecttags(@PathVariable String regex) {

        ModelMap modelMap = new ModelMap();
        modelMap.put(SUCCESS, true);
        modelMap.put(MSG, "get tags successfully");
        modelMap.put(DATA, flashDbService.searchTags(regex, DEFAULT_TAGS_LIMIT));
        return modelMap;

    }

    @ApiOperation(value = "batch saving points", notes = "batch saving points")
    @PostMapping(value = "/points")
    @ResponseBody
    public ModelMap batchInsertPoints(@RequestBody List<TagPointList> tagPointLists) {

        ModelMap modelMap = new ModelMap();

        if (CollectionUtils.isNotEmpty(tagPointLists)) {

            Set<String> tagCodeSet = tagPointLists.stream().map(p -> {
                return p.getTag();
            }).collect(Collectors.toSet());

            if (!flashDbService.checkTagList(tagCodeSet)) {

                modelMap.put(SUCCESS, false);
                modelMap.put(MSG, "tag should be created first");

            } else {

                if (flashDbService.saveDataPoints(tagPointLists) > 0) {

                    modelMap.put(SUCCESS, true);
                    modelMap.put(MSG, "save data successfully");

                }

            }

        } else {

            modelMap.put(SUCCESS, false);
            modelMap.put(MSG, "data list can't be empty");

        }

        return modelMap;
    }


    @ApiOperation(value = "search historcal data", notes = "search historcal data(support raw and interpolated)")
    @PostMapping(value = "/points/historcal")
    @ResponseBody
    public ModelMap historcalData(@RequestBody PointsSearchRequest pointsSearchRequest) {

        // todo, should think about limit parameter
        ModelMap modelMap = new ModelMap();

        Set<String> tagCodeSet = new HashSet<>(pointsSearchRequest.getTagList());

        if (!flashDbService.checkTagList(tagCodeSet)) {

            modelMap.put(SUCCESS, false);
            modelMap.put(MSG, "tag should be created first");

        } else if (pointsSearchRequest.getBgTime() > pointsSearchRequest.getEndTime()) {

            modelMap.put(SUCCESS, false);
            modelMap.put(MSG, "endtime should larger than bgtime");

        } else if (pointsSearchRequest.getSearchInterval() <= 0 &&
                pointsSearchRequest.getSearchMode().intValue() == PointsSearchMode.INTERPOLATED.getMode()) {

            modelMap.put(SUCCESS, false);
            modelMap.put(MSG, "search interval should be positive in interpolated mode");

        } else {

            modelMap.put(DATA, flashDbService.searchPoints(pointsSearchRequest));
            modelMap.put(SUCCESS, true);
            modelMap.put(MSG, "search points data successfully");

        }

        return modelMap;

    }

}
