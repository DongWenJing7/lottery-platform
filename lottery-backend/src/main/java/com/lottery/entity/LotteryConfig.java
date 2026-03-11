package com.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;

@TableName("lottery_config")
public class LotteryConfig {
    @TableId
    private Integer id;
    private Integer costTokens;
    private Integer jackpotThreshold;
    private String jackpotPrizeIds;

    public Integer getId() { return this.id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCostTokens() { return this.costTokens; }
    public void setCostTokens(Integer costTokens) { this.costTokens = costTokens; }
    public Integer getJackpotThreshold() { return this.jackpotThreshold; }
    public void setJackpotThreshold(Integer jackpotThreshold) { this.jackpotThreshold = jackpotThreshold; }
    public String getJackpotPrizeIds() { return this.jackpotPrizeIds; }
    public void setJackpotPrizeIds(String jackpotPrizeIds) { this.jackpotPrizeIds = jackpotPrizeIds; }
}
