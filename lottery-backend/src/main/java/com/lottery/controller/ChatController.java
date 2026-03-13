package com.lottery.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lottery.common.Result;
import com.lottery.entity.User;
import com.lottery.interceptor.AuthInterceptor;
import com.lottery.mapper.ChatMessageMapper;
import com.lottery.mapper.FriendshipMapper;
import com.lottery.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/player/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;
    private final FriendshipMapper friendshipMapper;

    @GetMapping("/conversations")
    public Result<?> getConversations() {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        return Result.success(chatMessageMapper.getConversations(userId));
    }

    @GetMapping("/messages")
    public Result<?> getMessages(@RequestParam Long userId,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        Long currentUserId = AuthInterceptor.CURRENT_USER_ID.get();
        int offset = (page - 1) * size;
        return Result.success(chatMessageMapper.getMessages(currentUserId, userId, offset, size));
    }

    @PostMapping("/read")
    public Result<?> markAsRead(@RequestParam Long userId) {
        Long currentUserId = AuthInterceptor.CURRENT_USER_ID.get();
        chatMessageMapper.markAsRead(userId, currentUserId);
        return Result.success();
    }

    @DeleteMapping("/conversation")
    public Result<?> deleteConversation(@RequestParam Long userId) {
        Long currentUserId = AuthInterceptor.CURRENT_USER_ID.get();
        chatMessageMapper.softDeleteConversation(currentUserId, userId);
        return Result.success();
    }

    @GetMapping("/unread-count")
    public Result<?> getUnreadCount() {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        return Result.success(chatMessageMapper.getUnreadCount(userId));
    }

    /**
     * Get chat status with target user.
     * Returns: canChat, isMarket, isFriend, hasSellerReplied, consecutiveCount
     */
    @GetMapping("/chat-status")
    public Result<?> getChatStatus(@RequestParam Long userId) {
        Long currentUserId = AuthInterceptor.CURRENT_USER_ID.get();
        boolean isFriend = friendshipMapper.areFriends(currentUserId, userId) != null;
        boolean isMarket = chatMessageMapper.countMarketMessages(currentUserId, userId) > 0;
        boolean hasReply = chatMessageMapper.countReplies(currentUserId, userId) > 0;
        int consecutive = isFriend ? 0 : chatMessageMapper.getConsecutiveCount(currentUserId, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("isFriend", isFriend);
        result.put("isMarket", isMarket);
        result.put("canChat", isFriend || isMarket);
        result.put("hasSellerReplied", hasReply);
        result.put("consecutiveCount", consecutive);
        return Result.success(result);
    }

    @GetMapping("/search")
    public Result<?> searchUsers(@RequestParam String keyword) {
        Long currentUserId = AuthInterceptor.CURRENT_USER_ID.get();
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .ne(User::getId, currentUserId)
                        .eq(User::getRole, "player")
                        .eq(User::getStatus, 1)
                        .and(w -> w.like(User::getUsername, keyword)
                                .or().like(User::getNickname, keyword))
                        .select(User::getId, User::getUsername, User::getNickname, User::getAvatar)
                        .last("LIMIT 20")
        );
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("nickname", u.getNickname() != null ? u.getNickname() : u.getUsername());
            map.put("avatar", u.getAvatar() != null ? u.getAvatar() : "");
            map.put("friendStatus", friendshipMapper.getFriendStatus(currentUserId, u.getId()));
            return map;
        }).collect(Collectors.toList());
        return Result.success(result);
    }
}
