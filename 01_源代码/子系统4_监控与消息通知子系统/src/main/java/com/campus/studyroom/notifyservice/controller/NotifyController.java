package com.campus.studyroom.notifyservice.controller;

import com.campus.studyroom.notifyservice.common.Result;
import com.campus.studyroom.notifyservice.security.JwtUtil;
import com.campus.studyroom.notifyservice.service.NotifyService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/notify")
public class NotifyController {

    @Autowired
    private NotifyService notifyService;

    @Autowired
    private JwtUtil jwtUtil;

    // --- 下面是前端调用的接口 ---

    // 4.3.1 消息列表
    @GetMapping("/messages")
    public Result<?> getMessages(@RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                 @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = getUserIdFromHeader(token);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        return notifyService.getMyMessages(userId, pageNum, pageSize);
    }

    // 4.3.2 标记已读
    @PostMapping("/messages/read")
    public Result<?> markRead(@RequestBody Map<String, List<Long>> body,
                              @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = getUserIdFromHeader(token);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        return notifyService.markAsRead(userId, body.get("messageIds"));
    }

    // 4.3.3 未读数
    @GetMapping("/messages/unread-count")
    public Result<?> getUnreadCount(@RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = getUserIdFromHeader(token);
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        return notifyService.getUnreadCount(userId);
    }

    // 提取公共的解析方法
    private Long getUserIdFromHeader(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return null;
        }
        try {
            // 成员1强调：必须去掉 "Bearer " 前缀
            String pureToken = token.replace("Bearer ", "");
            Claims claims = jwtUtil.parse(pureToken);
            return jwtUtil.getUserId(claims);
        } catch (Exception e) {
            // 包括 Token 过期或签名错误
            return null;
        }
    }
}