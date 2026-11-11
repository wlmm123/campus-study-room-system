package com.campus.studyroom.seat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.studyroom.seat.entity.SeatInfo;
import com.campus.studyroom.seat.entity.SeatOccupancy;
import com.campus.studyroom.seat.mapper.SeatInfoMapper;
import com.campus.studyroom.seat.mapper.SeatOccupancyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminSeatService {

    @Autowired
    private SeatInfoMapper seatInfoMapper;
    @Autowired
    private SeatOccupancyMapper seatOccupancyMapper;

    @Transactional
    public Long addSeat(Long roomId, String seatNumber) {
        Long count = seatInfoMapper.selectCount(new LambdaQueryWrapper<SeatInfo>().eq(SeatInfo::getRoomId, roomId).eq(SeatInfo::getSeatNumber, seatNumber).eq(SeatInfo::getIsDeleted, 0));
        if (count > 0) throw new IllegalArgumentException("座位号重复");
        SeatInfo seat = new SeatInfo();
        seat.setRoomId(roomId);
        seat.setSeatNumber(seatNumber);
        seat.setStatus("可用");
        seatInfoMapper.insert(seat);
        return seat.getId();
    }

    @Transactional
    public void updateSeat(Long seatId, String seatNumber) {
        SeatInfo seat = seatInfoMapper.selectById(seatId);
        Long count = seatInfoMapper.selectCount(new LambdaQueryWrapper<SeatInfo>().eq(SeatInfo::getRoomId, seat.getRoomId()).eq(SeatInfo::getSeatNumber, seatNumber).eq(SeatInfo::getIsDeleted, 0).ne(SeatInfo::getId, seatId));
        if (count > 0) throw new IllegalArgumentException("座位号重复");
        seat.setSeatNumber(seatNumber);
        seatInfoMapper.updateById(seat);
    }

    @Transactional
    public void deleteSeat(Long seatId) {
        Long occCount = seatOccupancyMapper.selectCount(new LambdaQueryWrapper<SeatOccupancy>().eq(SeatOccupancy::getSeatId, seatId).eq(SeatOccupancy::getReleaseStatus, 0));
        if (occCount > 0) throw new RuntimeException("1006:座位存在未释放的占用记录，无法删除");
        seatInfoMapper.deleteById(seatId);
    }

    /**
     * 调整座位状态（注意：所有复杂的业务拦截校验已经上移到 AdminController 中处理，
     * 这里只做纯粹的数据库更新操作）
     */
    @Transactional
    public void changeSeatStatus(Long seatId, String status) {
        // 校验传入的状态值是否合法
        if (!"可用".equals(status) && !"维护".equals(status) && !"停用".equals(status)) {
            throw new IllegalArgumentException("状态不合法");
        }

        // 直接更新状态
        SeatInfo seat = new SeatInfo();
        seat.setId(seatId);
        seat.setStatus(status);
        seatInfoMapper.updateById(seat);
    }
}