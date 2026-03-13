package com.lottery.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lottery.common.Result;
import com.lottery.entity.Friendship;
import com.lottery.entity.User;
import com.lottery.handler.ChatWebSocketHandler;
import com.lottery.interceptor.AuthInterceptor;
import com.lottery.mapper.FriendshipMapper;
import com.lottery.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/player/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendshipMapper friendshipMapper;
    private final UserMapper userMapper;

    @PostMapping("/request")
    public Result<?> sendRequest(@RequestParam Long targetId) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        if (userId.equals(targetId)) {
            return Result.error("不能添加自己为好友");
        }

        // Check if relationship already exists (either direction)
        Integer existing = friendshipMapper.getFriendStatus(userId, targetId);
        if (existing != null) {
            if (existing == 1) return Result.error("已经是好友了");
            if (existing == 0) return Result.error("已发送过好友申请，请等待对方确认");
            // If previously rejected, delete old record so we can create a new one
            friendshipMapper.deleteFriendship(userId, targetId);
        }

        Friendship f = new Friendship();
        f.setUserId(userId);
        f.setFriendId(targetId);
        f.setStatus(0);
        f.setCreatedAt(LocalDateTime.now());
        f.setUpdatedAt(LocalDateTime.now());
        friendshipMapper.insert(f);

        // Push WebSocket notification to target
        User sender = userMapper.selectById(userId);
        ChatWebSocketHandler.pushToUser(targetId, Map.of(
                "type", "friend_request",
                "userId", userId,
                "nickname", sender.getNickname() != null ? sender.getNickname() : sender.getUsername(),
                "avatar", sender.getAvatar() != null ? sender.getAvatar() : ""
        ));

        return Result.success();
    }

    @PostMapping("/accept")
    public Result<?> accept(@RequestParam Long id) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        Friendship f = friendshipMapper.selectById(id);
        if (f == null || !f.getFriendId().equals(userId) || f.getStatus() != 0) {
            return Result.error("无效的好友申请");
        }

        f.setStatus(1);
        f.setUpdatedAt(LocalDateTime.now());
        friendshipMapper.updateById(f);

        // Push notification to the requester
        User acceptor = userMapper.selectById(userId);
        ChatWebSocketHandler.pushToUser(f.getUserId(), Map.of(
                "type", "friend_accepted",
                "userId", userId,
                "nickname", acceptor.getNickname() != null ? acceptor.getNickname() : acceptor.getUsername()
        ));

        return Result.success();
    }

    @PostMapping("/reject")
    public Result<?> reject(@RequestParam Long id) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        Friendship f = friendshipMapper.selectById(id);
        if (f == null || !f.getFriendId().equals(userId) || f.getStatus() != 0) {
            return Result.error("无效的好友申请");
        }

        f.setStatus(2);
        f.setUpdatedAt(LocalDateTime.now());
        friendshipMapper.updateById(f);

        return Result.success();
    }

    @DeleteMapping("/{friendUserId}")
    public Result<?> deleteFriend(@PathVariable Long friendUserId) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        int deleted = friendshipMapper.deleteFriendship(userId, friendUserId);
        if (deleted == 0) {
            return Result.error("好友关系不存在");
        }
        return Result.success();
    }

    @GetMapping("/list")
    public Result<?> list() {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        return Result.success(friendshipMapper.getFriendList(userId));
    }

    @GetMapping("/requests")
    public Result<?> requests() {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        return Result.success(friendshipMapper.getPendingRequests(userId));
    }

    @GetMapping("/pending-count")
    public Result<?> pendingCount() {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        return Result.success(friendshipMapper.getPendingCount(userId));
    }
}
