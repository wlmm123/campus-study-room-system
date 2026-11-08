package com.campus.studyroom.notifyservice.mapper;

import com.campus.studyroom.notifyservice.entity.NotifyMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NotifyMessageMapper {
    // 根据 dedupKey 查消息（用于幂等判断）
    @Select("SELECT * FROM notify_message WHERE dedup_key = #{dedupKey} LIMIT 1")
    NotifyMessage selectByDedupKey(@Param("dedupKey") String dedupKey);

    // 4.3.1 分页查询当前用户的消息
    @Select("SELECT * FROM notify_message WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY create_time DESC LIMIT #{offset}, #{pageSize}")
    List<NotifyMessage> selectByUserId(@Param("userId") Long userId, @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    // 查询总数（配合分页）
    @Select("SELECT COUNT(*) FROM notify_message WHERE user_id = #{userId} AND is_deleted = 0")
    Integer countByUserId(@Param("userId") Long userId);

    // 4.3.2 标记已读
    @Update("UPDATE notify_message SET status = 'READ', read_time = NOW() WHERE user_id = #{userId} AND id IN (${ids})")
    void markAsRead(@Param("userId") Long userId, @Param("ids") String ids);

    // 4.3.3 查询未读数量
    @Select("SELECT COUNT(*) FROM notify_message WHERE user_id = #{userId} AND status = 'UNREAD' AND is_deleted = 0")
    Integer countUnread(@Param("userId") Long userId);

    // 插入消息
    void insert(NotifyMessage message);
}