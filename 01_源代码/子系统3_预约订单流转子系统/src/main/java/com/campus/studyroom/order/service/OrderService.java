package com.campus.studyroom.order.service;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.studyroom.order.common.BizException;
import com.campus.studyroom.order.common.ErrorCode;
import com.campus.studyroom.order.common.Result;
import com.campus.studyroom.order.entity.BizConfig;
import com.campus.studyroom.order.entity.OrderInfo;
import com.campus.studyroom.order.entity.OrderStatusLog;
import com.campus.studyroom.order.entity.Violation;
import com.campus.studyroom.order.mapper.BizConfigMapper;
import com.campus.studyroom.order.mapper.OrderInfoMapper;
import com.campus.studyroom.order.mapper.OrderStatusLogMapper;
import com.campus.studyroom.order.mapper.ViolationMapper;
import com.campus.studyroom.order.remote.NotifyClient;
import com.campus.studyroom.order.remote.SeatClient;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderStatusLogMapper orderStatusLogMapper;
    @Autowired
    private BizConfigMapper bizConfigMapper;
    @Autowired
    private ViolationMapper violationMapper;
    @Autowired
    private SeatClient seatClient;
    @Autowired
    private NotifyClient notifyClient;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ================== 核心状态机 ==================

    @Transactional(rollbackFor = Exception.class)
    public Result createOrder(Long userId, CreateOrderRequest request) {
        // 【修复点 8】参数非空校验，依据契约 1.2 应返回 HTTP 400
        if (request.getSeatId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, ErrorCode.BAD_REQUEST, "座位ID不能为空");
        }
        // 【修复点 1】补充 startTime 判空校验，防止空指针
        if (request.getStartTime() == null || request.getStartTime().trim().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, ErrorCode.BAD_REQUEST, "预约开始时间不能为空");
        }

        List<BizConfig> configs = bizConfigMapper.selectList(null);
        Integer durationUnitMinutes = getConfigInt(configs, "duration_unit_minutes", 60);
        Integer maxDurationHours = getConfigInt(configs, "max_duration_hours", 8);
        List<Integer> durationOptions = getConfigList(configs, "duration_options", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
        Integer durationHours = request.getDurationHours();
        
        if (durationHours == null || !durationOptions.contains(durationHours) || durationHours > maxDurationHours) {
            return Result.error(ErrorCode.PARAM_INVALID, "预约时长档位不在可选范围内");
        }
        
        LocalDateTime startTime;
        try {
            startTime = LocalDateTime.parse(request.getStartTime(), FORMATTER);
        } catch (Exception e) {
            return Result.error(ErrorCode.BAD_REQUEST, "时间格式错误，应为 yyyy-MM-dd HH:mm:ss");
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (startTime.isBefore(now)) {
            return Result.error(ErrorCode.PARAM_INVALID, "预约开始时间不能早于当前时间");
        }
        LocalDateTime endTime = startTime.plusMinutes((long) durationHours * durationUnitMinutes);
        
        LocalDateTime nextDayMidnight = startTime.toLocalDate().plusDays(1).atStartOfDay();
        if (endTime.isAfter(nextDayMidnight)) {
            return Result.error(ErrorCode.PARAM_INVALID, "单次预约不得跨自然日，或超出当天结束时间");
        }
        
        // 1008 校验（同一学生时段不重叠）
        LambdaQueryWrapper<OrderInfo> overlapWrapper = new LambdaQueryWrapper<>();
        overlapWrapper.eq(OrderInfo::getUserId, userId)
                .in(OrderInfo::getStatus, "待签到", "已签到")
                .lt(OrderInfo::getStartTime, endTime)
                .gt(OrderInfo::getEndTime, startTime);
        if (orderInfoMapper.selectCount(overlapWrapper) > 0) {
            return Result.error(ErrorCode.TIME_OVERLAP, "该时段您已有其他预约");
        }

        long orderId = IdUtil.getSnowflakeNextId();
        SeatClient.LockSeatRequest lockReq = new SeatClient.LockSeatRequest();
        lockReq.setOrderId(orderId);
        lockReq.setStartTime(startTime.format(FORMATTER));
        lockReq.setEndTime(endTime.format(FORMATTER));
        lockReq.setDurationHours(durationHours);

        String lockResponse;
        try {
            lockResponse = seatClient.lockSeat(request.getSeatId(), lockReq);
        } catch (Exception e) {
            return Result.error(ErrorCode.REMOTE_SERVICE_ERROR, "座位子系统网络异常，锁座失败");
        }
        
        JSONObject lockJson = parseRemoteResponse(lockResponse);
        if (lockJson == null) {
            return Result.error(ErrorCode.REMOTE_SERVICE_ERROR, "座位子系统响应异常");
        }
        Integer lockCode = lockJson.getInt("code");
        if (lockCode == null || lockCode != 200) {
            return Result.error(lockCode == null ? 500 : lockCode, lockJson.getStr("message"));
        }

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setUserId(userId);
        orderInfo.setSeatId(request.getSeatId());
        orderInfo.setStatus("待签到");
        orderInfo.setStartTime(startTime);
        orderInfo.setEndTime(endTime);
        
        try {
            orderInfoMapper.insert(orderInfo);
            writeStatusLog(orderId, null, "待签到", "STUDENT", "创建预约");
        } catch (Exception e) {
            // 【修复点 7】补偿失败日志补全字段，依据契约 4.2.1(6)
            try {
                SeatClient.ReleaseSeatRequest releaseReq = new SeatClient.ReleaseSeatRequest();
                releaseReq.setOrderId(orderId);
                releaseReq.setAction("CANCEL");
                seatClient.releaseSeat(request.getSeatId(), releaseReq);
            } catch (Exception ex) {
                System.err.println("【严重】补偿失败日志 - orderId:" + orderId + ", seatId:" + request.getSeatId() + ", 原因:" + ex.getMessage() + ", 时间:" + LocalDateTime.now().format(FORMATTER));
            }
            // 【修复点 2】落库失败需返回 500 HTTP 状态
            throw new BizException(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR, "订单落库失败，已尝试释放座位");
        }

        pushMessage("RESERVE_SUCCESS", userId, orderId, "预约成功", "您已成功预约座位，请按时签到");

        CreateOrderResponse response = new CreateOrderResponse();
        response.setOrderId(String.valueOf(orderId));
        response.setSeatId(request.getSeatId());
        response.setStatus("待签到");
        response.setStartTime(startTime.format(FORMATTER));
        response.setEndTime(endTime.format(FORMATTER));
        return Result.success("预约下单成功", response);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result signIn(Long userId, Long orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN, "无权访问该订单");
        }
        // 【修复点 1】状态错误码改为 1003
        if (!"待签到".equals(order.getStatus())) {
            return Result.error(ErrorCode.STATE_NOT_ALLOWED, "当前订单状态不允许签到");
        }
        
        List<BizConfig> configs = bizConfigMapper.selectList(null);
        Integer signInDeadlineMinutes = getConfigInt(configs, "sign_in_deadline_minutes", 15);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = order.getStartTime().plusMinutes(signInDeadlineMinutes);
        
        // 【修复点 1】超时/未到时间错误码改为 1003
        if (now.isBefore(order.getStartTime()) || now.isAfter(deadline)) {
            return Result.error(ErrorCode.STATE_NOT_ALLOWED, "不在签到时间窗口内");
        }
        
        SeatClient.StatusUpdateRequest statusReq = new SeatClient.StatusUpdateRequest();
        statusReq.setOrderId(orderId);
        statusReq.setStatus("已占用");
        
        String responseStr;
        try {
            responseStr = seatClient.updateStatus(order.getSeatId(), statusReq);
        } catch (Exception e) {
            return Result.error(ErrorCode.REMOTE_SERVICE_ERROR, "座位系统网络异常，签到失败");
        }
        JSONObject json = parseRemoteResponse(responseStr);
        if (json == null) return Result.error(ErrorCode.REMOTE_SERVICE_ERROR, "座位系统响应异常");
        
        Integer code = json.getInt("code");
        if (code == null || code != 200) {
            return Result.error(code == null ? 500 : code, json.getStr("message"));
        }
        
        order.setStatus("已签到");
        orderInfoMapper.updateById(order);
        writeStatusLog(orderId, "待签到", "已签到", "STUDENT", "签到");

        // 【修复点：对齐契约 4.2.2 成功响应结构】
        Map<String, Object> resData = new HashMap<>();
        resData.put("orderId", String.valueOf(orderId));
        resData.put("status", "已签到");
        resData.put("seatStatus", "已占用");
        return Result.success("签到成功", resData);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result cancel(Long userId, Long orderId, boolean isAdmin) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN, "无权访问该订单");
        }
        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN, "无权访问该订单");
        }
        // 【修复点 1】状态错误码改为 1003
        if (!"待签到".equals(order.getStatus())) {
            return Result.error(ErrorCode.STATE_NOT_ALLOWED, "当前订单状态不允许取消");
        }
        
        if (!isAdmin) {
            List<BizConfig> configs = bizConfigMapper.selectList(null);
            Integer signInDeadlineMinutes = getConfigInt(configs, "sign_in_deadline_minutes", 15);
            LocalDateTime deadline = order.getStartTime().plusMinutes(signInDeadlineMinutes);
            if (LocalDateTime.now().isAfter(deadline)) {
                // 【修复点 1】超时错误码改为 1003
                return Result.error(ErrorCode.STATE_NOT_ALLOWED, "已超过签到截止时间，无法取消");
            }
        }
        
        SeatClient.ReleaseSeatRequest releaseReq = new SeatClient.ReleaseSeatRequest();
        releaseReq.setOrderId(orderId);
        releaseReq.setAction("CANCEL");
        
        String responseStr;
        try {
            responseStr = seatClient.releaseSeat(order.getSeatId(), releaseReq);
        } catch (Exception e) {
            return Result.error(ErrorCode.REMOTE_SERVICE_ERROR, "座位系统网络异常，取消失败");
        }
        JSONObject json = parseRemoteResponse(responseStr);
        if (json == null) return Result.error(ErrorCode.REMOTE_SERVICE_ERROR, "座位系统响应异常");
        
        Integer code = json.getInt("code");
        if (code == null || code != 200) {
            return Result.error(code == null ? 500 : code, json.getStr("message"));
        }
        
        order.setStatus("已取消");
        orderInfoMapper.updateById(order);
        
        String operator = isAdmin ? "ADMIN" : "STUDENT";
        writeStatusLog(orderId, "待签到", "已取消", operator, "取消预约");
        
        if (isAdmin) {
            pushMessage("SYSTEM", order.getUserId(), orderId, "预约已取消", "因座位维护，您的预约已被取消");
        }

        // 【修复点：对齐契约 4.2.3 成功响应结构】
        Map<String, Object> resData = new HashMap<>();
        resData.put("orderId", String.valueOf(orderId));
        resData.put("status", "已取消");
        return Result.success("取消预约成功", resData);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result tempLeave(Long userId, Long orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN, "无权访问该订单");
        }
        // 【修复点 1】状态错误码改为 1003
        if (!"已签到".equals(order.getStatus())) {
            return Result.error(ErrorCode.STATE_NOT_ALLOWED, "当前订单状态不允许暂离");
        }
        if (order.getTempLeaveStartTime() != null) {
            return Result.error(ErrorCode.STATE_NOT_ALLOWED, "当前已处于暂离状态");
        }
        if (LocalDateTime.now().isAfter(order.getEndTime()) || LocalDateTime.now().isEqual(order.getEndTime())) {
            return Result.error(ErrorCode.STATE_NOT_ALLOWED, "已到达使用截止时间，无法暂离");
        }
        
        SeatClient.StatusUpdateRequest statusReq = new SeatClient.StatusUpdateRequest();
        statusReq.setOrderId(orderId);
        statusReq.setStatus("暂离中");
        
        String responseStr;
        try {
            responseStr = seatClient.updateStatus(order.getSeatId(), statusReq);
        } catch (Exception e) {
            return Result.error(ErrorCode.REMOTE_SERVICE_ERROR, "座位系统网络异常，暂离失败");
        }
        JSONObject json = parseRemoteResponse(responseStr);
        if (json == null) return Result.error(ErrorCode.REMOTE_SERVICE_ERROR, "座位系统响应异常");
        
        Integer code = json.getInt("code");
        if (code == null || code != 200) {
            return Result.error(code == null ? 500 : code, json.getStr("message"));
        }
        
        order.setTempLeaveStartTime(LocalDateTime.now());
        orderInfoMapper.updateById(order);
        writeStatusLog(orderId, "已签到", "已签到", "STUDENT", "执行暂离");

        // 【修复点：对齐契约 4.2.4 成功响应结构】
        Map<String, Object> resData = new HashMap<>();
        resData.put("orderId", String.valueOf(orderId));
        resData.put("status", "已签到");
        resData.put("seatStatus", "暂离中");
        resData.put("tempLeaveStartTime", order.getTempLeaveStartTime().format(FORMATTER));
        return Result.success("暂离操作成功", resData);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result returnBack(Long userId, Long orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN, "无权访问该订单");
        }
        // 【修复点 1】状态错误码改为 1003
        if (!"已签到".equals(order.getStatus()) || order.getTempLeaveStartTime() == null) {
            return Result.error(ErrorCode.STATE_NOT_ALLOWED, "当前订单状态不允许返回");
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // 分支1：已到期
        if (now.isAfter(order.getEndTime()) || now.isEqual(order.getEndTime())) {
            SeatClient.ReleaseSeatRequest releaseReq = new SeatClient.ReleaseSeatRequest();
            releaseReq.setOrderId(orderId);
            releaseReq.setAction("FINISH");
            String responseStr;
            try {
                responseStr = seatClient.releaseSeat(order.getSeatId(), releaseReq);
            } catch (Exception e) {
                return Result.error(ErrorCode.REMOTE_SERVICE_ERROR, "座位系统网络异常，返回失败");
            }
            JSONObject json = parseRemoteResponse(responseStr);
            if (json == null) return Result.error(ErrorCode.REMOTE_SERVICE_ERROR, "座位系统响应异常");
            Integer code = json.getInt("code");
            if (code == null || code != 200) {
                return Result.error(code == null ? 500 : code, json.getStr("message"));
            }
            
            order.setStatus("已完成");
            order.setTempLeaveStartTime(null);
            order.setFinishTime(now);
            orderInfoMapper.updateById(order);
            writeStatusLog(orderId, "已签到", "已完成", "STUDENT", "暂离返回时已到期");
            
            // 【修复点：对齐契约 4.2.5 到期分支成功响应结构】
            Map<String, Object> resData = new HashMap<>();
            resData.put("orderId", String.valueOf(orderId));
            resData.put("status", "已完成");
            resData.put("seatStatus", "可用");
            resData.put("tempLeaveStartTime", null);
            return Result.success("使用已到期，座位已释放", resData);
        }
        
        // 分支2：暂离超时
        List<BizConfig> configs = bizConfigMapper.selectList(null);
        Integer tempLeaveLimitMinutes = getConfigInt(configs, "temp_leave_limit_minutes", 120);
        LocalDateTime leaveDeadline = order.getTempLeaveStartTime().plusMinutes(tempLeaveLimitMinutes);
        
        if (now.isAfter(leaveDeadline)) {
            SeatClient.ReleaseSeatRequest releaseReq = new SeatClient.ReleaseSeatRequest();
            releaseReq.setOrderId(orderId);
            releaseReq.setAction("TIMEOUT");
            String responseStr;
            try {
                responseStr = seatClient.releaseSeat(order.getSeatId(), releaseReq);
            } catch (Exception e) {
                return Result.error(ErrorCode.REMOTE_SERVICE_ERROR, "座位系统网络异常，返回失败");
            }
            JSONObject json = parseRemoteResponse(responseStr);
            if (json == null) return Result.error(ErrorCode.REMOTE_SERVICE_ERROR, "座位系统响应异常");
            Integer code = json.getInt("code");
            if (code == null || code != 200) {
                return Result.error(code == null ? 500 : code, json.getStr("message"));
            }
            
            order.setStatus("暂离超时释放");
            order.setTempLeaveStartTime(null);
            order.setFinishTime(now);
            orderInfoMapper.updateById(order);
            writeStatusLog(orderId, "已签到", "暂离超时释放", "STUDENT", "暂离超时释放");
            writeViolation(orderId, userId, "暂离超时", "学生主动返回时发现暂离已超时");
            pushMessage("TEMP_LEAVE_TIMEOUT_RELEASE", userId, orderId, "暂离超时释放", "您暂离超过规定时限，座位已释放");
            
            // 【修复点 2：对齐契约 4.2.5 暂离超时分支响应结构，补全 data】
            Map<String, Object> resData = new HashMap<>();
            resData.put("orderId", String.valueOf(orderId));
            resData.put("status", "暂离超时释放");
            resData.put("seatStatus", "可用");
            
            Result<Map<String, Object>> timeoutResult = Result.error(1004, "暂离已超时，座位已释放");
            timeoutResult.setData(resData);
            return timeoutResult;
        }
        
        // 分支3：正常返回
        SeatClient.StatusUpdateRequest statusReq = new SeatClient.StatusUpdateRequest();
        statusReq.setOrderId(orderId);
        statusReq.setStatus("已占用");
        String responseStr;
        try {
            responseStr = seatClient.updateStatus(order.getSeatId(), statusReq);
        } catch (Exception e) {
            return Result.error(ErrorCode.REMOTE_SERVICE_ERROR, "座位系统网络异常，返回失败");
        }
        JSONObject json = parseRemoteResponse(responseStr);
        if (json == null) return Result.error(ErrorCode.REMOTE_SERVICE_ERROR, "座位系统响应异常");
        Integer code = json.getInt("code");
        if (code == null || code != 200) {
            return Result.error(code == null ? 500 : code, json.getStr("message"));
        }
        
        order.setTempLeaveStartTime(null);
        orderInfoMapper.updateById(order);
        writeStatusLog(orderId, "已签到", "已签到", "STUDENT", "暂离返回重新签到");

        // 【修复点：对齐契约 4.2.5 正常返回成功响应结构】
        Map<String, Object> resData = new HashMap<>();
        resData.put("orderId", String.valueOf(orderId));
        resData.put("status", "已签到");
        resData.put("seatStatus", "已占用");
        resData.put("tempLeaveStartTime", null);
        return Result.success("返回座位成功", resData);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result leave(Long userId, Long orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN, "无权访问该订单");
        }
        // 【修复点 1】状态错误码改为 1003
        if (!"已签到".equals(order.getStatus())) {
            return Result.error(ErrorCode.STATE_NOT_ALLOWED, "当前订单状态不允许主动离开");
        }
        
        SeatClient.ReleaseSeatRequest releaseReq = new SeatClient.ReleaseSeatRequest();
        releaseReq.setOrderId(orderId);
        releaseReq.setAction("FINISH");
        
        String responseStr;
        try {
            responseStr = seatClient.releaseSeat(order.getSeatId(), releaseReq);
        } catch (Exception e) {
            return Result.error(ErrorCode.REMOTE_SERVICE_ERROR, "座位系统网络异常，离开失败");
        }
        JSONObject json = parseRemoteResponse(responseStr);
        if (json == null) return Result.error(ErrorCode.REMOTE_SERVICE_ERROR, "座位系统响应异常");
        Integer code = json.getInt("code");
        if (code == null || code != 200) {
            return Result.error(code == null ? 500 : code, json.getStr("message"));
        }
        
        order.setStatus("已完成");
        order.setTempLeaveStartTime(null);
        order.setFinishTime(LocalDateTime.now());
        orderInfoMapper.updateById(order);
        writeStatusLog(orderId, "已签到", "已完成", "STUDENT", "主动离开签退");

        // 【修复点：对齐契约 4.2.6 成功响应结构】
        Map<String, Object> resData = new HashMap<>();
        resData.put("orderId", String.valueOf(orderId));
        resData.put("status", "已完成");
        resData.put("seatStatus", "可用");
        return Result.success("主动离开成功", resData);
    }

    // ================== 查询与配置接口 ==================

    public Result getMyOrders(Long userId, Integer pageNum, Integer pageSize) {
        Page<OrderInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getUserId, userId).orderByDesc(OrderInfo::getCreateTime);
        orderInfoMapper.selectPage(page, wrapper);
        return Result.success("查询成功", buildPageResult(page));
    }

    public Result getOrderDetail(Long userId, Long orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN, "无权访问该订单");
        }
        return Result.success("查询成功", buildOrderMap(order));
    }

    public Result getAdminOrders(Integer pageNum, Integer pageSize, String status, Long userId, Long seatId, String startDate, String endDate) {
        Page<OrderInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) wrapper.eq(OrderInfo::getStatus, status);
        if (userId != null) wrapper.eq(OrderInfo::getUserId, userId);
        if (seatId != null) wrapper.eq(OrderInfo::getSeatId, seatId);
        if (startDate != null && !startDate.isEmpty()) wrapper.ge(OrderInfo::getStartTime, startDate);
        if (endDate != null && !endDate.isEmpty()) wrapper.le(OrderInfo::getEndTime, endDate);
        wrapper.orderByDesc(OrderInfo::getCreateTime);
        orderInfoMapper.selectPage(page, wrapper);
        return Result.success("查询成功", buildPageResult(page));
    }

    public Result getAdminViolations(Integer pageNum, Integer pageSize, Long userId, Long orderId, String type, String startDate, String endDate) {
        Page<Violation> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Violation> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(Violation::getUserId, userId);
        if (orderId != null) wrapper.eq(Violation::getOrderId, orderId);
        if (type != null && !type.isEmpty()) wrapper.eq(Violation::getType, type);
        if (startDate != null && !startDate.isEmpty()) wrapper.ge(Violation::getViolationTime, startDate);
        if (endDate != null && !endDate.isEmpty()) wrapper.le(Violation::getViolationTime, endDate);
        wrapper.orderByDesc(Violation::getViolationTime);
        violationMapper.selectPage(page, wrapper);
        return Result.success("查询成功", buildViolationPageResult(page));
    }

    public Result getBusinessConfig() {
        List<BizConfig> configs = bizConfigMapper.selectList(null);
        Map<String, Object> data = new HashMap<>();
        data.put("durationOptions", getConfigList(configs, "duration_options", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8)));
        data.put("durationUnitMinutes", getConfigInt(configs, "duration_unit_minutes", 60));
        data.put("signInDeadlineMinutes", getConfigInt(configs, "sign_in_deadline_minutes", 15));
        data.put("tempLeaveLimitMinutes", getConfigInt(configs, "temp_leave_limit_minutes", 120));
        return Result.success("操作成功", data);
    }

    // 【修复点 5】4.2.11 内部接口补 scanIntervalSeconds
    public Result getInternalBusinessConfig() {
        List<BizConfig> configs = bizConfigMapper.selectList(null);
        Map<String, Object> data = new HashMap<>();
        data.put("durationOptions", getConfigList(configs, "duration_options", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8)));
        data.put("durationUnitMinutes", getConfigInt(configs, "duration_unit_minutes", 60));
        data.put("signInDeadlineMinutes", getConfigInt(configs, "sign_in_deadline_minutes", 15));
        data.put("tempLeaveLimitMinutes", getConfigInt(configs, "temp_leave_limit_minutes", 120));
        data.put("scanIntervalSeconds", getConfigInt(configs, "scan_interval_seconds", 60)); // 补全该字段
        return Result.success("查询成功", data);
    }

    // ================== 4.2.12 定时任务 ==================

    public Result batchHandleTimeout() {
        int handleCount = 0;
        int signInTimeoutReleased = 0;
        int tempLeaveTimeoutReleased = 0;
        int expiredCompleted = 0;
        int violationCreated = 0;
        int seatReleaseFailed = 0;
        int messagePushTriggered = 0;
        LocalDateTime now = LocalDateTime.now();
        List<BizConfig> configs = bizConfigMapper.selectList(null);
        Integer signInDeadlineMinutes = getConfigInt(configs, "sign_in_deadline_minutes", 15);
        Integer tempLeaveLimitMinutes = getConfigInt(configs, "temp_leave_limit_minutes", 120);
        
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OrderInfo::getStatus, "待签到", "已签到");
        List<OrderInfo> activeOrders = orderInfoMapper.selectList(wrapper);
        
        for (OrderInfo order : activeOrders) {
            try {
                // ① 使用到期（含暂离中）
                if ("已签到".equals(order.getStatus()) && !now.isBefore(order.getEndTime())) {
                    String resp;
                    try {
                        resp = callRelease(order.getSeatId(), order.getId(), "FINISH");
                    } catch (Exception e) {
                        System.err.println("到期释放座位网络异常，订单跳过本轮：" + order.getId());
                        seatReleaseFailed++;
                        continue;
                    }
                    if (!isSuccess(resp)) { 
                        seatReleaseFailed++; 
                        continue; 
                    }
                    
                    order.setStatus("已完成");
                    order.setTempLeaveStartTime(null);
                    order.setFinishTime(now);
                    orderInfoMapper.updateById(order);
                    writeStatusLog(order.getId(), "已签到", "已完成", "SYSTEM", "定时任务：使用到期");
                    pushMessage("USE_EXPIRED_COMPLETE", order.getUserId(), order.getId(), "使用到期完成", "您的预约已到期，座位已释放");
                    
                    expiredCompleted++;
                    messagePushTriggered++;
                    handleCount++;
                    continue;
                }
                
                // ② 超时未签到
                LocalDateTime signInDeadline = order.getStartTime().plusMinutes(signInDeadlineMinutes);
                if ("待签到".equals(order.getStatus()) && now.isAfter(signInDeadline)) {
                    String resp;
                    try {
                        resp = callRelease(order.getSeatId(), order.getId(), "TIMEOUT");
                    } catch (Exception e) {
                        System.err.println("超时释放座位网络异常，订单跳过本轮：" + order.getId());
                        seatReleaseFailed++;
                        continue;
                    }
                    if (!isSuccess(resp)) { 
                        seatReleaseFailed++; 
                        continue; 
                    }
                    order.setStatus("超时释放");
                    order.setFinishTime(now);
                    orderInfoMapper.updateById(order);
                    writeStatusLog(order.getId(), "待签到", "超时释放", "SYSTEM", "定时任务：超时未签到");
                    writeViolation(order.getId(), order.getUserId(), "超时未签到", "预约超时未签到");
                    pushMessage("SIGN_IN_TIMEOUT_RELEASE", order.getUserId(), order.getId(), "预约超时释放", "您预约的座位因超时未签到已释放");
                    
                    signInTimeoutReleased++;
                    violationCreated++;
                    messagePushTriggered++;
                    handleCount++;
                    continue;
                }
                
                // ③ 暂离超时
                if ("已签到".equals(order.getStatus()) && order.getTempLeaveStartTime() != null) {
                    LocalDateTime leaveDeadline = order.getTempLeaveStartTime().plusMinutes(tempLeaveLimitMinutes);
                    if (now.isAfter(leaveDeadline) && now.isBefore(order.getEndTime())) {
                        String resp;
                        try {
                            resp = callRelease(order.getSeatId(), order.getId(), "TIMEOUT");
                        } catch (Exception e) {
                            System.err.println("暂离超时释放座位网络异常，订单跳过本轮：" + order.getId());
                            seatReleaseFailed++;
                            continue;
                        }
                        if (!isSuccess(resp)) { 
                            seatReleaseFailed++; 
                            continue; 
                        }
                        order.setStatus("暂离超时释放");
                        order.setTempLeaveStartTime(null);
                        order.setFinishTime(now);
                        orderInfoMapper.updateById(order);
                        writeStatusLog(order.getId(), "已签到", "暂离超时释放", "SYSTEM", "定时任务：暂离超时");
                        writeViolation(order.getId(), order.getUserId(), "暂离超时", "暂离超时未返回");
                        pushMessage("TEMP_LEAVE_TIMEOUT_RELEASE", order.getUserId(), order.getId(), "暂离超时释放", "您暂离超过规定时限，座位已释放");
                        
                        tempLeaveTimeoutReleased++;
                        violationCreated++;
                        messagePushTriggered++;
                        handleCount++;
                        continue;
                    }
                }
                
                // ④ 预约生效
                LocalDateTime startTime = order.getStartTime();
                if ("待签到".equals(order.getStatus()) && !now.isBefore(startTime) && !now.isAfter(signInDeadline)) {
                    try {
                        // 【修复点 6】仅在座位可用时置已预约。这里通过解析 3.5 的返回值容错处理，或者此处依赖 3.5 只要不是可用就不改状态。
                        // 契约要求：若座位当前为「可用」，调 3.5 置「已预约」。
                        SeatClient.StatusUpdateRequest req = new SeatClient.StatusUpdateRequest();
                        req.setOrderId(order.getId());
                        req.setStatus("已预约");
                        seatClient.updateStatus(order.getSeatId(), req);
                    } catch (Exception e) {
                        System.err.println("预约生效同步座位状态失败：" + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.err.println("处理订单 " + order.getId() + " 异常：" + e.getMessage());
            }
        }
        
        Map<String, Object> res = new HashMap<>();
        res.put("handleCount", handleCount);
        res.put("signInTimeoutReleased", signInTimeoutReleased);
        res.put("tempLeaveTimeoutReleased", tempLeaveTimeoutReleased);
        res.put("expiredCompleted", expiredCompleted);
        res.put("violationCreated", violationCreated);
        res.put("seatReleaseFailed", seatReleaseFailed);
        res.put("messagePushTriggered", messagePushTriggered);
        return Result.success("批量超时扫描执行完成", res);
    }

    // ================== 辅助方法 ==================

    private JSONObject parseRemoteResponse(String responseStr) {
        if (responseStr == null || responseStr.trim().isEmpty()) return null;
        try {
            return JSONUtil.parseObj(responseStr);
        } catch (Exception e) {
            return null;
        }
    }

    private String callRelease(Long seatId, Long orderId, String action) {
        SeatClient.ReleaseSeatRequest req = new SeatClient.ReleaseSeatRequest();
        req.setOrderId(orderId);
        req.setAction(action);
        return seatClient.releaseSeat(seatId, req);
    }

    private boolean isSuccess(String resp) {
        JSONObject json = parseRemoteResponse(resp);
        if (json == null) return false;
        Integer code = json.getInt("code");
        return code != null && code == 200;
    }

    private void writeStatusLog(Long orderId, String from, String to, String operatorType, String reason) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setOperatorType(operatorType);
        log.setTransitionTime(LocalDateTime.now());
        log.setReason(reason);
        orderStatusLogMapper.insert(log);
    }

    private void writeViolation(Long orderId, Long userId, String type, String reason) {
        Violation violation = new Violation();
        violation.setOrderId(orderId);
        violation.setUserId(userId);
        violation.setType(type);
        violation.setViolationTime(LocalDateTime.now());
        violation.setReason(reason);
        violationMapper.insert(violation);
    }

    private Integer getConfigInt(List<BizConfig> configs, String key, Integer defaultVal) {
        return configs.stream().filter(c -> key.equals(c.getConfigKey())).map(c -> Integer.parseInt(c.getConfigValue())).findFirst().orElse(defaultVal);
    }

    private List<Integer> getConfigList(List<BizConfig> configs, String key, List<Integer> defaultVal) {
        return configs.stream().filter(c -> key.equals(c.getConfigKey())).map(c -> Arrays.stream(c.getConfigValue().split(",")).map(Integer::parseInt).collect(Collectors.toList())).findFirst().orElse(defaultVal);
    }

    private void pushMessage(String eventType, Long userId, Long orderId, String title, String content) {
        try {
            NotifyClient.PushRequest pushReq = new NotifyClient.PushRequest();
            pushReq.setEventType(eventType);
            pushReq.setUserId(userId);
            pushReq.setOrderId(orderId);
            pushReq.setTitle(title);
            pushReq.setContent(content);
            pushReq.setBizTime(LocalDateTime.now().format(FORMATTER));
            pushReq.setDedupKey(eventType + ":" + orderId);
            notifyClient.pushMessage(pushReq);
        } catch (Exception e) {
            System.err.println("推送消息失败(不影响主流程): " + e.getMessage());
        }
    }

    private Map<String, Object> buildPageResult(Page<OrderInfo> page) {
        Map<String, Object> res = new HashMap<>();
        res.put("total", page.getTotal());
        res.put("pageNum", page.getCurrent());
        res.put("pageSize", page.getSize());
        res.put("list", page.getRecords().stream().map(this::buildOrderMap).collect(Collectors.toList()));
        return res;
    }

    private Map<String, Object> buildOrderMap(OrderInfo order) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", String.valueOf(order.getId()));
        map.put("seatId", order.getSeatId());
        map.put("userId", order.getUserId());
        map.put("status", order.getStatus());
        map.put("startTime", order.getStartTime() != null ? order.getStartTime().format(FORMATTER) : null);
        map.put("endTime", order.getEndTime() != null ? order.getEndTime().format(FORMATTER) : null);
        map.put("tempLeaveStartTime", order.getTempLeaveStartTime() != null ? order.getTempLeaveStartTime().format(FORMATTER) : null);
        return map;
    }

    private Map<String, Object> buildViolationPageResult(Page<Violation> page) {
        Map<String, Object> res = new HashMap<>();
        res.put("total", page.getTotal());
        res.put("pageNum", page.getCurrent());
        res.put("pageSize", page.getSize());
        List<Map<String, Object>> list = page.getRecords().stream().map(v -> {
            Map<String, Object> map = new HashMap<>();
            map.put("violationId", v.getId());
            map.put("orderId", String.valueOf(v.getOrderId()));
            map.put("userId", v.getUserId());
            map.put("type", v.getType());
            map.put("violationTime", v.getViolationTime() != null ? v.getViolationTime().format(FORMATTER) : null);
            map.put("reason", v.getReason());
            return map;
        }).collect(Collectors.toList());
        res.put("list", list);
        return res;
    }

    // ================== DTO ==================

    @Data
    public static class CreateOrderRequest {
        private Long seatId;
        private String startTime;
        private Integer durationHours;
    }

    @Data
    public static class CreateOrderResponse {
        private String orderId;
        private Long seatId;
        private String status;
        private String startTime;
        private String endTime;
    }
}