package com.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("market")
public class Market {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long warehouseId;
    private Long sellerId;
    private Integer price;
    private String status;     // on / sold / off
    private Long buyerId;
    private LocalDateTime createdAt;
    private LocalDateTime soldAt;

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public Long getWarehouseId() { return this.warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public Long getSellerId() { return this.sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public Integer getPrice() { return this.price; }
    public void setPrice(Integer price) { this.price = price; }
    public String getStatus() { return this.status; }
    public void setStatus(String status) { this.status = status; }
    public Long getBuyerId() { return this.buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getSoldAt() { return this.soldAt; }
    public void setSoldAt(LocalDateTime soldAt) { this.soldAt = soldAt; }
}
