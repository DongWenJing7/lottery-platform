package com.lottery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lottery.entity.Friendship;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface FriendshipMapper extends BaseMapper<Friendship> {

    @Select("SELECT u.id, u.username, u.nickname, u.avatar FROM friendship f " +
            "JOIN user u ON u.id = CASE WHEN f.user_id = #{userId} THEN f.friend_id ELSE f.user_id END " +
            "WHERE ((f.user_id = #{userId}) OR (f.friend_id = #{userId})) AND f.status = 1")
    List<Map<String, Object>> getFriendList(@Param("userId") Long userId);

    @Select("SELECT f.id, f.user_id AS userId, u.nickname, u.avatar, f.created_at AS createdAt " +
            "FROM friendship f JOIN user u ON u.id = f.user_id " +
            "WHERE f.friend_id = #{userId} AND f.status = 0 ORDER BY f.created_at DESC")
    List<Map<String, Object>> getPendingRequests(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM friendship WHERE friend_id = #{userId} AND status = 0")
    int getPendingCount(@Param("userId") Long userId);

    @Select("SELECT status FROM friendship WHERE " +
            "((user_id = #{userId} AND friend_id = #{friendId}) OR (user_id = #{friendId} AND friend_id = #{userId})) LIMIT 1")
    Integer getFriendStatus(@Param("userId") Long userId, @Param("friendId") Long friendId);

    @Select("SELECT id FROM friendship WHERE " +
            "((user_id = #{userId} AND friend_id = #{friendId}) OR (user_id = #{friendId} AND friend_id = #{userId})) " +
            "AND status = 1 LIMIT 1")
    Long areFriends(@Param("userId") Long userId, @Param("friendId") Long friendId);

    @Delete("DELETE FROM friendship WHERE " +
            "((user_id = #{userId} AND friend_id = #{friendId}) OR (user_id = #{friendId} AND friend_id = #{userId}))")
    int deleteFriendship(@Param("userId") Long userId, @Param("friendId") Long friendId);
}
