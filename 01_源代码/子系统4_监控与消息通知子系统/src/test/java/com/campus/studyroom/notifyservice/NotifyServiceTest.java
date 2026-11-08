package com.campus.studyroom.notifyservice;

import com.campus.studyroom.notifyservice.entity.NotifyMessage;
import com.campus.studyroom.notifyservice.mapper.NotifyMessageMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest
public class NotifyServiceTest {

    @Autowired
    private NotifyMessageMapper messageMapper;

    @Test
    public void testInsertAndQuery() {
        NotifyMessage msg = new NotifyMessage();
        msg.setUserId(99L);
        msg.setEventType("SYSTEM");
        msg.setTitle("单元测试消息");
        msg.setContent("这是JUnit测试生成的");
        msg.setStatus("UNREAD");
        msg.setBizTime(LocalDateTime.now());
        msg.setDedupKey("TEST_UNIT_KEY_" + System.currentTimeMillis());

        // 插入
        messageMapper.insert(msg);

        // 查询验证
        NotifyMessage result = messageMapper.selectByDedupKey(msg.getDedupKey());
        System.out.println("查询到的标题：" + result.getTitle());
    }
}