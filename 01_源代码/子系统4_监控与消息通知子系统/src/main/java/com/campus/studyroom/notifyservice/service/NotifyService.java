package com.campus.studyroom.notifyservice.service;

import com.campus.studyroom.notifyservice.common.Result;
import com.campus.studyroom.notifyservice.entity.NotifyMessage;
import com.campus.studyroom.notifyservice.mapper.NotifyMessageMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotifyService {

    @Autowired
    private NotifyMessageMapper messageMapper;

    public Result<?> pushMessage(PushRequest req) {
        // 1. 幂等校验
        NotifyMessage exist = messageMapper.selectByDedupKey(req.getDedupKey());
        if (exist != null) {
            return Result.success(new PushResponse(exist.getId(), true)); // 已存在，返回 duplicated=true
        }

        // 2. 插入新消息
        NotifyMessage msg = new NotifyMessage();
        msg.setUserId(req.getUserId());
        msg.setOrderId(req.getOrderId());
        msg.setEventType(req.getEventType());
        msg.setTitle(req.getTitle());
        msg.setContent(req.getContent());
        msg.setStatus("UNREAD");
        msg.setBizTime(req.getBizTime());
        msg.setDedupKey(req.getDedupKey());

        messageMapper.insert(msg);

        return Result.success(new PushResponse(msg.getId(), false));
    }

    @Data
    public static class PushRequest {
        private String eventType;
        private Long userId;
        private Long orderId;
        private String title;
        private String content;

        // 加上这个注解
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime bizTime;
        private String dedupKey;
    }

    @Data
    public static class PushResponse {
        private Long messageId;
        private Boolean duplicated;
        public PushResponse(Long messageId, Boolean duplicated) {
            this.messageId = messageId;
            this.duplicated = duplicated;
        }
    }

    // 分页查询
    public Result<?> getMyMessages(Long userId, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<NotifyMessage> list = messageMapper.selectByUserId(userId, offset, pageSize);
        Integer total = messageMapper.countByUserId(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        data.put("list", list);
        return Result.success(data);
    }

    // 标记已读
    public Result<?> markAsRead(Long userId, List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) return Result.error(400, "参数不能为空");
        // 将 List 转为 "1,2,3" 的字符串用于 IN 查询
        String ids = messageIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        messageMapper.markAsRead(userId, ids);
        return Result.success(null);
    }

    // 未读数量
    public Result<?> getUnreadCount(Long userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("unreadCount", messageMapper.countUnread(userId));
        return Result.success(data);
    }
}