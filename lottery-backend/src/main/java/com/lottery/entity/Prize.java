package com.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("prize")
public class Prize {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String type;
    private String image;
    private Double value;
    private Double probability;
    private Integer isJackpot;
    private Integer recycleTokens;
    private Integer tokenReward;
    private Integer stock;
    private Integer status;
    private LocalDateTime createdAt;

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return this.name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return this.type; }
    public void setType(String type) { this.type = type; }
    public String getImage() { return this.image; }
    public void setImage(String image) { this.image = image; }
    public Double getValue() { return this.value; }
    public void setValue(Double value) { this.value = value; }
    public Double getProbability() { return this.probability; }
    public void setProbability(Double probability) { this.probability = probability; }
    public Integer getIsJackpot() { return this.isJackpot; }
    public void setIsJackpot(Integer isJackpot) { this.isJackpot = isJackpot; }
    public Integer getRecycleTokens() { return this.recycleTokens; }
    public void setRecycleTokens(Integer recycleTokens) { this.recycleTokens = recycleTokens; }
    public Integer getTokenReward() { return this.tokenReward; }
    public void setTokenReward(Integer tokenReward) { this.tokenReward = tokenReward; }
    public Integer getStock() { return this.stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getStatus() { return this.status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
