package com.lottery.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA 路由支持：将所有非 API、非静态资源请求转发到 index.html，
 * 让 Vue Router 的 history 模式在刷新时正常工作。
 */
@Controller
public class SpaController {

    @RequestMapping(value = {"/login", "/register", "/player/**", "/agent/**", "/admin/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
