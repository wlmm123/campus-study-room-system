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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminRoomService {

    @Autowired
    private RoomInfoMapper roomInfoMapper;
    @Autowired
    private SeatInfoMapper seatInfoMapper;
    @Autowired
    private SeatOccupancyMapper seatOccupancyMapper;

    @Transactional
    public Long addRoom(RoomInfo room) {
        Long count = roomInfoMapper.selectCount(new LambdaQueryWrapper<RoomInfo>()
                .eq(RoomInfo::getRoomName, room.getRoomName())
                .eq(RoomInfo::getIsDeleted, 0));
        if (count > 0) throw new IllegalArgumentException("自习室名称重复");

        // 确保默认状态和删除标记正确
        if (room.getStatus() == null) room.setStatus(1);
        room.setIsDeleted(0);
        roomInfoMapper.insert(room);
        return room.getId();
    }

    @Transactional
    public void updateRoom(Long roomId, RoomInfo room) {
        Long count = roomInfoMapper.selectCount(new LambdaQueryWrapper<RoomInfo>()
                .eq(RoomInfo::getRoomName, room.getRoomName())
                .eq(RoomInfo::getIsDeleted, 0)
                .ne(RoomInfo::getId, roomId));
        if (count > 0) throw new IllegalArgumentException("自习室名称重复");

        room.setId(roomId);
        roomInfoMapper.updateById(room);
    }

    @Transactional
    public void deleteRoom(Long roomId) {
        List<SeatInfo> seats = seatInfoMapper.selectList(new LambdaQueryWrapper<SeatInfo>()
                .eq(SeatInfo::getRoomId, roomId)
                .eq(SeatInfo::getIsDeleted, 0));

        if (!seats.isEmpty()) {
            List<Long> seatIds = seats.stream().map(SeatInfo::getId).collect(Collectors.toList());

            Long occCount = seatOccupancyMapper.selectCount(new LambdaQueryWrapper<SeatOccupancy>()
                    .in(SeatOccupancy::getSeatId, seatIds)
                    .eq(SeatOccupancy::getReleaseStatus, 0));

            if (occCount > 0) {
                throw new RuntimeException("1006:资源存在未释放的占用记录，无法删除");
            }
        }

        RoomInfo room = new RoomInfo();
        room.setId(roomId);
        room.setIsDeleted(1);
        roomInfoMapper.updateById(room);

        // 座位也逻辑删除
        if (!seats.isEmpty()) {
            for (SeatInfo s : seats) {
                s.setIsDeleted(1);
                seatInfoMapper.updateById(s);
            }
        }
    }
}