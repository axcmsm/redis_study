package com.axcmsm.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * this class is for Axcmsm
 *
 * @author 须贺
 * @version 2023/4/1 22:40
 */
@Controller
public class IndexController {

    @GetMapping("/{page}.html")
    public String index(@PathVariable("page") String  page){
        return page;
    }
}
