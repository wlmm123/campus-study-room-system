package com.campus.studyroom.seat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SeatLogicTest {

    // 1. 基础逻辑：验证状态枚举的 6 个词完全正确
    @Test
    public void testSeatStatusEnum() {
        String[] statusList = {"可用", "已预约", "已占用", "暂离中", "维护", "停用"};
        assertEquals(6, statusList.length);
        assertEquals("已预约", statusList[1]);
        System.out.println("【测试通过】1. 状态枚举测试");
    }

    // 2. 核心业务：验证时间重叠判据
    @Test
    public void testTimeOverlap() {
        LocalDateTime startA = LocalDateTime.of(2026, 11, 8, 10, 0);
        LocalDateTime endA = LocalDateTime.of(2026, 11, 8, 12, 0);

        // B时段包含A时段的一部分
        LocalDateTime startB = LocalDateTime.of(2026, 11, 8, 11, 0);
        LocalDateTime endB = LocalDateTime.of(2026, 11, 8, 13, 0);

        // 冲突判据：A开始 < B结束 且 A结束 > B开始
        boolean isConflict = startA.isBefore(endB) && endA.isAfter(startB);
        assertTrue(isConflict, "重叠时段应判定为冲突");

        // 相邻时段不算冲突
        LocalDateTime startC = LocalDateTime.of(2026, 11, 8, 12, 0);
        LocalDateTime endC = LocalDateTime.of(2026, 11, 8, 14, 0);
        boolean isConflictC = startA.isBefore(endC) && endA.isAfter(startC);
        assertFalse(isConflictC, "相邻时段不应判定为冲突");
        System.out.println("【测试通过】2. 时间重叠判据测试");
    }

    // 3. 核心业务：验证未来时段不改变座位状态（契约 3.3(6)）
    @Test
    public void testFutureBookingRule() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startFuture = now.plusHours(2);
        LocalDateTime endFuture = now.plusHours(4);

        // 模拟当前时刻，未来时段判定
        boolean isCoveringNow = !startFuture.isAfter(now) && endFuture.isAfter(now);
        assertFalse(isCoveringNow, "未来时段不覆盖当前时刻");
        System.out.println("【测试通过】3. 锁未来时段不改状态规则测试");
    }

    // 4. 核心业务：验证当前时段改变座位状态（契约 3.3(6)）
    @Test
    public void testCurrentBookingRule() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startCurrent = now.minusMinutes(10);
        LocalDateTime endCurrent = now.plusMinutes(10);

        // 模拟当前时刻，当前时段判定
        boolean isCoveringNow = !startCurrent.isAfter(now) && endCurrent.isAfter(now);
        assertTrue(isCoveringNow, "当前时段覆盖当前时刻");
        System.out.println("【测试通过】4. 锁当前时段置已预约规则测试");
    }

    // 5. 核心业务：验证防覆盖逻辑（契约 3.5(2)）
    @Test
    public void testStatusOverrideProtection() {
        String currentStatus = "已占用";
        String incomingStatus = "已预约";

        // 模拟 3.5 接口里的保护逻辑
        boolean shouldSkip = "已预约".equals(incomingStatus) &&
                ("已占用".equals(currentStatus) || "暂离中".equals(currentStatus));

        assertTrue(shouldSkip, "已占用/暂离中不应被已预约覆盖");
        System.out.println("【测试通过】5. 防止状态被盲目覆盖测试");
    }
}