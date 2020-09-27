package com.offcn.sellergoods.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/login")
public class LoginController {
    //获取当前登陆的用户信息
    @RequestMapping("/name")
    public Map name(){
        Map map= new HashMap();
       String name= SecurityContextHolder.getContext().getAuthentication().getName();
        map.put("loginName",name);
        return  map;
    }
}
