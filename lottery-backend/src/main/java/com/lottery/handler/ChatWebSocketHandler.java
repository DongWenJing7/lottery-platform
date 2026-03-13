package com.lottery.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.entity.ChatMessage;
import com.lottery.entity.User;
import com.lottery.mapper.ChatMessageMapper;
import com.lottery.mapper.FriendshipMapper;
import com.lottery.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;
    private final FriendshipMapper friendshipMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final ConcurrentHashMap<Long, WebSocketSession> onlineSessions = new ConcurrentHashMap<>();
    private static final ObjectMapper staticMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            onlineSessions.put(userId, session);
            log.info("WebSocket connected: userId={}", userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long senderId = (Long) session.getAttributes().get("userId");
        if (senderId == null) return;

        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
        Long receiverId = Long.valueOf(payload.get("receiverId").toString());
        String content = (String) payload.get("content");

        if (content == null || content.trim().isEmpty() || content.length() > 500) return;

        boolean isMarketMsg = payload.get("isMarket") != null && Boolean.TRUE.equals(payload.get("isMarket"));
        Long areFriends = friendshipMapper.areFriends(senderId, receiverId);
        boolean isMarketConv = chatMessageMapper.countMarketMessages(senderId, receiverId) > 0;

        if (areFriends == null && !isMarketConv && !isMarketMsg) {
            // Not friends and not a market conversation - block
            Map<String, Object> err = Map.of("type", "error", "message", "对方不是你的好友");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(err)));
            return;
        }

        if (areFriends == null && (isMarketConv || isMarketMsg)) {
            // Market conversation - check if seller has replied
            int sellerReplies = chatMessageMapper.countReplies(senderId, receiverId);
            if (sellerReplies == 0) {
                // Seller hasn't replied yet - buyer limited to 3 messages
                int consecutive = chatMessageMapper.getConsecutiveCount(senderId, receiverId);
                if (consecutive >= 3) {
                    Map<String, Object> err = Map.of("type", "error", "message", "已达上限，等待对方回复后可继续发送");
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(err)));
                    return;
                }
            }
        }

        // Save to DB
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSenderId(senderId);
        chatMessage.setReceiverId(receiverId);
        chatMessage.setContent(content.trim());
        chatMessage.setIsRead(0);
        chatMessage.setIsMarket(isMarketMsg ? 1 : 0);
        chatMessage.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(chatMessage);

        // Build response
        User sender = userMapper.selectById(senderId);
        Map<String, Object> resp = Map.of(
                "id", chatMessage.getId(),
                "senderId", senderId,
                "senderName", sender.getNickname() != null ? sender.getNickname() : sender.getUsername(),
                "senderAvatar", sender.getAvatar() != null ? sender.getAvatar() : "",
                "content", chatMessage.getContent(),
                "createdAt", chatMessage.getCreatedAt().toString()
        );
        String json = objectMapper.writeValueAsString(resp);

        // Push to receiver if online
        WebSocketSession receiverSession = onlineSessions.get(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            try {
                receiverSession.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.error("Failed to send message to user {}", receiverId, e);
            }
        }

        // Echo back to sender for confirmation
        session.sendMessage(new TextMessage(json));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            onlineSessions.remove(userId, session);
            log.info("WebSocket disconnected: userId={}", userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            onlineSessions.remove(userId, session);
        }
        log.error("WebSocket transport error", exception);
    }

    /**
     * Push a JSON message to a specific online user via WebSocket.
     * Used by FriendController to send friend request/accept notifications.
     */
    public static void pushToUser(Long userId, Map<String, Object> data) {
        WebSocketSession session = onlineSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(staticMapper.writeValueAsString(data)));
            } catch (IOException e) {
                log.error("Failed to push to user {}", userId, e);
            }
        }
    }
}
