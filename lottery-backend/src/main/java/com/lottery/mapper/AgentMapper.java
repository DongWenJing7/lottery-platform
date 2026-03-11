package com.lottery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lottery.entity.Agent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AgentMapper extends BaseMapper<Agent> {
    @Select("SELECT * FROM agent WHERE user_id = #{userId}")
    Agent findByUserId(Long userId);

    @Select("SELECT * FROM agent WHERE invite_code = #{inviteCode}")
    Agent findByInviteCode(String inviteCode);
}
