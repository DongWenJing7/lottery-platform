package com.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String address;
    private String role;        // player / agent / admin
    private Integer balance;
    private Long agentId;
    private Integer status;     // 1正常 0封禁
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return this.username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return this.password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return this.nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return this.avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getAddress() { return this.address; }
    public void setAddress(String address) { this.address = address; }
    public String getRole() { return this.role; }
    public void setRole(String role) { this.role = role; }
    public Integer getBalance() { return this.balance; }
    public void setBalance(Integer balance) { this.balance = balance; }
    public Long getAgentId() { return this.agentId; }
    public void setAgentId(Long agentId) { this.agentId = agentId; }
    public Integer getStatus() { return this.status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return this.updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
