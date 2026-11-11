package com.campus.studyroom.seat.controller;

import com.campus.studyroom.seat.common.Result;
import com.campus.studyroom.seat.service.RoomSeatQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin
public class RoomSeatController {

    @Autowired
    private RoomSeatQueryService queryService;

    @GetMapping
    public Result<?> getRooms(@RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize,
                              HttpServletRequest request) {
        List<?> roles = (List<?>) request.getAttribute("roles");
        boolean isAdmin = roles != null && roles.contains("ADMIN");
        return Result.success(queryService.getRooms(pageNum, pageSize, isAdmin));
    }

    @GetMapping("/{roomId}/seats")
    public Result<?> getSeats(@PathVariable Long roomId,
                              @RequestParam(required = false) String startTime,
                              @RequestParam(required = false) String endTime,
                              HttpServletRequest request) {
        List<?> roles = (List<?>) request.getAttribute("roles");
        boolean isAdmin = roles != null && roles.contains("ADMIN");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime start = startTime != null ? LocalDateTime.parse(startTime, formatter) : null;
        LocalDateTime end = endTime != null ? LocalDateTime.parse(endTime, formatter) : null;

        return Result.success(queryService.getSeats(roomId, start, end, isAdmin));
    }
}