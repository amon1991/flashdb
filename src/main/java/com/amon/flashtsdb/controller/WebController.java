package com.amon.flashtsdb.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author yaming.chen@siemens.com
 * Created by chenyaming on 2020/10/20.
 */
@RestController
@RequestMapping(value = "/portal")
public class WebController {

    @RequestMapping("/index")
    public ModelAndView index() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("index");
        return mv;
    }

}
