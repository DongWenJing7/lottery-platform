package com.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("after_sale")
public class AfterSale {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long deliveryOrderId;
    private Long userId;
    private String type;       // refund / exchange
    private String reason;
    private String status;     // pending / approved / rejected / done
    private String rejectReason;
    private Long operatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return this.orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getDeliveryOrderId() { return this.deliveryOrderId; }
    public void setDeliveryOrderId(Long deliveryOrderId) { this.deliveryOrderId = deliveryOrderId; }
    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return this.type; }
    public void setType(String type) { this.type = type; }
    public String getReason() { return this.reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return this.status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectReason() { return this.rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public Long getOperatorId() { return this.operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return this.updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
