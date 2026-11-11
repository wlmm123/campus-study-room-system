package com.campus.studyroom.seat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.studyroom.seat.entity.RoomInfo;
import com.campus.studyroom.seat.entity.SeatInfo;
import com.campus.studyroom.seat.entity.SeatOccupancy;
import com.campus.studyroom.seat.mapper.RoomInfoMapper;
import com.campus.studyroom.seat.mapper.SeatInfoMapper;
import com.campus.studyroom.seat.mapper.SeatOccupancyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoomSeatQueryService {

    @Autowired
    private RoomInfoMapper roomInfoMapper;
    @Autowired
    private SeatInfoMapper seatInfoMapper;
    @Autowired
    private SeatOccupancyMapper seatOccupancyMapper;

    public Map<String, Object> getRooms(int pageNum, int pageSize, boolean isAdmin) {
        LambdaQueryWrapper<RoomInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomInfo::getIsDeleted, 0);
        if (!isAdmin) {
            wrapper.eq(RoomInfo::getStatus, 1);
        }
        List<RoomInfo> list = roomInfoMapper.selectList(wrapper);

        int from = Math.min((pageNum - 1) * pageSize, list.size());
        int to = Math.min(from + pageSize, list.size());

        Map<String, Object> res = new HashMap<>();
        res.put("total", list.size());
        res.put("pageNum", pageNum);
        res.put("pageSize", pageSize);
        res.put("list", list.subList(from, to));
        return res;
    }

    public Map<String, Object> getSeats(Long roomId, LocalDateTime startTime, LocalDateTime endTime, boolean isAdmin) {
        RoomInfo room = roomInfoMapper.selectById(roomId);
        if (room == null || room.getIsDeleted() == 1) {
            throw new RuntimeException("1006:自习室不存在");
        }
        if (!isAdmin && room.getStatus() == 0) {
            throw new RuntimeException("1006:自习室已关闭");
        }

        List<SeatInfo> seats = seatInfoMapper.selectList(
                new LambdaQueryWrapper<SeatInfo>().eq(SeatInfo::getRoomId, roomId).eq(SeatInfo::getIsDeleted, 0)
        );

        List<Map<String, Object>> resultList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (SeatInfo seat : seats) {
            Map<String, Object> map = new HashMap<>();
            map.put("seatId", seat.getId());
            map.put("seatNumber", seat.getSeatNumber());
            map.put("status", seat.getStatus());

            if (startTime == null || endTime == null) {
                map.put("bookable", null);
            } else {
                boolean bookable = true;
                if ("维护".equals(seat.getStatus()) || "停用".equals(seat.getStatus())) {
                    bookable = false;
                }
                if (startTime.isBefore(now)) {
                    bookable = false;
                }
                if (bookable) {
                    Long conflict = seatOccupancyMapper.selectCount(
                            new LambdaQueryWrapper<SeatOccupancy>()
                                    .eq(SeatOccupancy::getSeatId, seat.getId())
                                    .eq(SeatOccupancy::getReleaseStatus, 0)
                                    .lt(SeatOccupancy::getStartTime, endTime)
                                    .gt(SeatOccupancy::getEndTime, startTime)
                    );
                    if (conflict > 0) {
                        bookable = false;
                    }
                }
                map.put("bookable", bookable);
            }
            resultList.add(map);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("list", resultList);
        return res;
    }
}