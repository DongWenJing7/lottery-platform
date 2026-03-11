package com.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;

@TableName("system_config")
public class SystemConfig {
    @TableId(type = IdType.INPUT)
    private String configKey;
    private String configValue;

    public String getConfigKey() { return this.configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return this.configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
}
