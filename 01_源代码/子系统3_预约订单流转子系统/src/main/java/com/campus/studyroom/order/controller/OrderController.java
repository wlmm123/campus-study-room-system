package com.campus.studyroom.order.controller;

import com.campus.studyroom.order.common.BizException;
import com.campus.studyroom.order.common.ErrorCode;
import com.campus.studyroom.order.common.Result;
import com.campus.studyroom.order.service.OrderService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.Key;

@RestController
@CrossOrigin
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    // 从 Token 解析 userId（【修复点 9】缺失抛 401）
    private Long getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BizException(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Token 缺失或格式非法");
        }
        String token = authHeader.substring(7);
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return Long.valueOf(claims.get("userId").toString());
    }

    // 从 Token 判断是否管理员（【修复点 9】缺失抛 401）
    private boolean isAdmin(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BizException(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Token 缺失");
        }
        String token = authHeader.substring(7);
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        Object rolesObj = claims.get("roles");
        return rolesObj != null && rolesObj.toString().contains("ADMIN");
    }

    // ================== 核心状态机接口 ==================

    @PostMapping("/api/orders/reserve")
    public Result createOrder(@RequestBody OrderService.CreateOrderRequest request, HttpServletRequest httpRequest) {
        Long currentUserId = getUserIdFromToken(httpRequest);
        return orderService.createOrder(currentUserId, request);
    }

    @PostMapping("/api/orders/{orderId}/sign-in")
    public Result signIn(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        Long currentUserId = getUserIdFromToken(httpRequest);
        return orderService.signIn(currentUserId, orderId);
    }

    @PostMapping("/api/orders/{orderId}/cancel")
    public Result cancel(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        Long currentUserId = getUserIdFromToken(httpRequest);
        boolean admin = isAdmin(httpRequest);
        return orderService.cancel(currentUserId, orderId, admin);
    }

    @PostMapping("/api/orders/{orderId}/temp-leave")
    public Result tempLeave(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        Long currentUserId = getUserIdFromToken(httpRequest);
        return orderService.tempLeave(currentUserId, orderId);
    }

    @PostMapping("/api/orders/{orderId}/return-back")
    public Result returnBack(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        Long currentUserId = getUserIdFromToken(httpRequest);
        return orderService.returnBack(currentUserId, orderId);
    }

    @PostMapping("/api/orders/{orderId}/leave")
    public Result leave(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        Long currentUserId = getUserIdFromToken(httpRequest);
        return orderService.leave(currentUserId, orderId);
    }

    // ================== 查询接口 ==================

    @GetMapping("/api/orders/my-list")
    public Result getMyOrders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest httpRequest) {
        Long currentUserId = getUserIdFromToken(httpRequest);
        return orderService.getMyOrders(currentUserId, pageNum, pageSize);
    }

    @GetMapping("/api/orders/{orderId}/detail")
    public Result getOrderDetail(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        Long currentUserId = getUserIdFromToken(httpRequest);
        return orderService.getOrderDetail(currentUserId, orderId);
    }

    @GetMapping("/api/admin/orders")
    public Result getAdminOrders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long seatId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            // 【修复点 2】HTTP 403 需抛异常
            throw new BizException(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN, "无权限访问");
        }
        return orderService.getAdminOrders(pageNum, pageSize, status, userId, seatId, startDate, endDate);
    }

    @GetMapping("/api/admin/violations")
    public Result getAdminViolations(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            throw new BizException(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN, "无权限访问");
        }
        return orderService.getAdminViolations(pageNum, pageSize, userId, orderId, type, startDate, endDate);
    }
}