package com.lottery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lottery.entity.ChatMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("""
        SELECT
            CASE WHEN cm.sender_id = #{userId} THEN cm.receiver_id ELSE cm.sender_id END AS userId,
            u.nickname, u.avatar,
            cm.content AS lastMessage, cm.created_at AS lastTime,
            (SELECT COUNT(*) FROM chat_message
             WHERE sender_id = CASE WHEN cm.sender_id = #{userId} THEN cm.receiver_id ELSE cm.sender_id END
             AND receiver_id = #{userId} AND is_read = 0
             AND created_at > COALESCE((SELECT deleted_at FROM chat_conversation_delete
                 WHERE user_id = #{userId} AND target_id = CASE WHEN cm.sender_id = #{userId} THEN cm.receiver_id ELSE cm.sender_id END), '1970-01-01')
            ) AS unreadCount
        FROM chat_message cm
        JOIN user u ON u.id = CASE WHEN cm.sender_id = #{userId} THEN cm.receiver_id ELSE cm.sender_id END
        WHERE cm.id IN (
            SELECT MAX(sub.id) FROM chat_message sub
            WHERE (sub.sender_id = #{userId} OR sub.receiver_id = #{userId})
            AND sub.created_at > COALESCE((SELECT deleted_at FROM chat_conversation_delete
                WHERE user_id = #{userId} AND target_id = CASE WHEN sub.sender_id = #{userId} THEN sub.receiver_id ELSE sub.sender_id END), '1970-01-01')
            GROUP BY CASE WHEN sub.sender_id = #{userId} THEN sub.receiver_id ELSE sub.sender_id END
        )
        ORDER BY cm.created_at DESC
    """)
    List<Map<String, Object>> getConversations(@Param("userId") Long userId);

    @Select("""
        SELECT cm.id, cm.sender_id AS senderId, cm.receiver_id AS receiverId,
               cm.content, cm.is_read AS isRead, cm.created_at AS createdAt,
               u.nickname AS senderName, u.avatar AS senderAvatar
        FROM chat_message cm
        JOIN user u ON u.id = cm.sender_id
        WHERE ((cm.sender_id = #{userId} AND cm.receiver_id = #{targetId})
           OR (cm.sender_id = #{targetId} AND cm.receiver_id = #{userId}))
        AND cm.created_at > COALESCE((SELECT deleted_at FROM chat_conversation_delete
            WHERE user_id = #{userId} AND target_id = #{targetId}), '1970-01-01')
        ORDER BY cm.created_at DESC
        LIMIT #{offset}, #{size}
    """)
    List<Map<String, Object>> getMessages(@Param("userId") Long userId,
                                           @Param("targetId") Long targetId,
                                           @Param("offset") int offset,
                                           @Param("size") int size);

    @Update("UPDATE chat_message SET is_read = 1 WHERE sender_id = #{senderId} AND receiver_id = #{receiverId} AND is_read = 0")
    int markAsRead(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    @Select("""
        SELECT COUNT(*) FROM chat_message cm
        WHERE cm.receiver_id = #{userId} AND cm.is_read = 0
        AND cm.created_at > COALESCE((SELECT deleted_at FROM chat_conversation_delete
            WHERE user_id = #{userId} AND target_id = cm.sender_id), '1970-01-01')
    """)
    int getUnreadCount(@Param("userId") Long userId);

    @Insert("""
        INSERT INTO chat_conversation_delete (user_id, target_id, deleted_at)
        VALUES (#{userId}, #{targetId}, NOW())
        ON DUPLICATE KEY UPDATE deleted_at = NOW()
    """)
    int softDeleteConversation(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * Count consecutive messages from senderId at the tail of their conversation.
     * Counts sender's messages after the last message from the other side.
     * Not affected by soft delete - counts actual DB records.
     */
    /** Check if this is a market-initiated conversation */
    @Select("""
        SELECT COUNT(*) FROM chat_message
        WHERE ((sender_id = #{userId} AND receiver_id = #{targetId})
           OR (sender_id = #{targetId} AND receiver_id = #{userId}))
        AND is_market = 1
    """)
    int countMarketMessages(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /** Count sender's consecutive messages after the last message from the other side */
    @Select("""
        SELECT COUNT(*) FROM chat_message
        WHERE sender_id = #{senderId} AND receiver_id = #{receiverId}
        AND id > COALESCE(
            (SELECT MAX(id) FROM chat_message WHERE sender_id = #{receiverId} AND receiver_id = #{senderId}), 0
        )
    """)
    int getConsecutiveCount(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    /** Check if the receiver (seller) has ever replied in this conversation */
    @Select("""
        SELECT COUNT(*) FROM chat_message
        WHERE sender_id = #{receiverId} AND receiver_id = #{senderId}
    """)
    int countReplies(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);
}
