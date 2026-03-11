package com.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("agent")
public class Agent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String inviteCode;       // 随机邀请码
    private Integer level;           // 0无 1V1 2V2 3V3
    private Integer teamCount;
    private Integer teamRecharge;
    private BigDecimal commissionRate;
    private BigDecimal totalCommission;
    private LocalDateTime updatedAt;

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getLevel() { return this.level; }
    public void setLevel(Integer level) { this.level = level; }
    public Integer getTeamCount() { return this.teamCount; }
    public void setTeamCount(Integer teamCount) { this.teamCount = teamCount; }
    public Integer getTeamRecharge() { return this.teamRecharge; }
    public void setTeamRecharge(Integer teamRecharge) { this.teamRecharge = teamRecharge; }
    public BigDecimal getCommissionRate() { return this.commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
    public BigDecimal getTotalCommission() { return this.totalCommission; }
    public void setTotalCommission(BigDecimal totalCommission) { this.totalCommission = totalCommission; }
    public String getInviteCode() { return this.inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public LocalDateTime getUpdatedAt() { return this.updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
