package com.campus.studyroom.seat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.studyroom.seat.entity.SeatInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SeatInfoMapper extends BaseMapper<SeatInfo> {

    @Select("SELECT * FROM seat_info WHERE id = #{id} FOR UPDATE")
    SeatInfo selectByIdForUpdate(@Param("id") Long id);

}