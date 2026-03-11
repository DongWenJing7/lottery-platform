package com.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("recharge_order")
public class RechargeOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Integer tokens;
    private Double amount;
    private String remark;
    private String status;    // pending / done / rejected
    private String rejectReason;
    private Long operatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return this.orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getTokens() { return this.tokens; }
    public void setTokens(Integer tokens) { this.tokens = tokens; }
    public Double getAmount() { return this.amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getRemark() { return this.remark; }
    public void setRemark(String remark) { this.remark = remark; }
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
