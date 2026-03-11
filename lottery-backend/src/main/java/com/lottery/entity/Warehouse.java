package com.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("warehouse")
public class Warehouse {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long prizeId;
    private String status;   // held / selling / recycled / delivered
    private Integer sellPrice;
    private LocalDateTime createdAt;

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPrizeId() { return this.prizeId; }
    public void setPrizeId(Long prizeId) { this.prizeId = prizeId; }
    public String getStatus() { return this.status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getSellPrice() { return this.sellPrice; }
    public void setSellPrice(Integer sellPrice) { this.sellPrice = sellPrice; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
