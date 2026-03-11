package com.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("token_log")
public class TokenLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer amount;
    private Integer balanceAfter;
    private String type;       // recharge/draw/recycle/sell/buy/manual
    private Long refId;
    private String remark;
    private LocalDateTime createdAt;

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getAmount() { return this.amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public Integer getBalanceAfter() { return this.balanceAfter; }
    public void setBalanceAfter(Integer balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getType() { return this.type; }
    public void setType(String type) { this.type = type; }
    public Long getRefId() { return this.refId; }
    public void setRefId(Long refId) { this.refId = refId; }
    public String getRemark() { return this.remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
