package com.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("draw_record")
public class DrawRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long prizeId;
    private Integer isJackpot;
    private Integer consecutiveCount;
    private LocalDateTime createdAt;

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPrizeId() { return this.prizeId; }
    public void setPrizeId(Long prizeId) { this.prizeId = prizeId; }
    public Integer getIsJackpot() { return this.isJackpot; }
    public void setIsJackpot(Integer isJackpot) { this.isJackpot = isJackpot; }
    public Integer getConsecutiveCount() { return this.consecutiveCount; }
    public void setConsecutiveCount(Integer consecutiveCount) { this.consecutiveCount = consecutiveCount; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
