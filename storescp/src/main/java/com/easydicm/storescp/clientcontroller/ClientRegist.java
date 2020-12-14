package com.easydicm.storescp.clientcontroller;

import com.easydicm.storescp.models.RegInfo;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@RestController
@RequestMapping(value = "api")

public class ClientRegist {

    private  final  Logger logger = LogManager.getLogger(this.getClass());
    private  final  String  regFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    @PostMapping(value = "regist")
    @ResponseBody
    public RegInfo  client(@RequestBody RegInfo regInfo){

        logger.info(  "注册客户端!"+ regInfo.getClientId());

        return   regInfo;
    }

    @RequestMapping("echo")
    public String echo() {

        LocalDateTime dt = LocalDateTime.now(); // 当前日期和时间
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(regFormat);
        return dtf.format(dt);
    }
}
