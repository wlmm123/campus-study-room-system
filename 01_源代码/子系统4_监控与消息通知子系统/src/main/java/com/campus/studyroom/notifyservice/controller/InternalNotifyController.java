package com.campus.studyroom.notifyservice.controller;

import com.campus.studyroom.notifyservice.common.Result;
import com.campus.studyroom.notifyservice.service.NotifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/internal/notify") // 内部路径
public class InternalNotifyController {

    @Autowired
    private NotifyService notifyService;

    // 4.3.4 内部消息推送
    @PostMapping("/push")
    public Result<?> pushMessage(@RequestBody NotifyService.PushRequest request) {
        return notifyService.pushMessage(request);
    }
}