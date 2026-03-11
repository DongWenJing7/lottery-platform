package com.lottery.scheduler;

import com.lottery.entity.Agent;
import com.lottery.mapper.AgentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentLevelScheduler {

    private final AgentMapper agentMapper;

    /**
     * 每日凌晨 2:00 执行代理等级判定
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void evaluateAgentLevels() {
        log.info("开始执行代理等级判定...");
        List<Agent> agents = agentMapper.selectList(null);
        int updated = 0;

        for (Agent agent : agents) {
            int teamCount = agent.getTeamCount() != null ? agent.getTeamCount() : 0;
            int teamRecharge = agent.getTeamRecharge() != null ? agent.getTeamRecharge() : 0;

            int newLevel;
            BigDecimal newRate;

            if (teamCount >= 50 || teamRecharge >= 5000) {
                newLevel = 3;
                newRate = new BigDecimal("3.00");
            } else if (teamCount >= 30 || teamRecharge >= 3000) {
                newLevel = 2;
                newRate = new BigDecimal("2.00");
            } else if (teamCount >= 10 || teamRecharge >= 1000) {
                newLevel = 1;
                newRate = new BigDecimal("1.00");
            } else {
                newLevel = 0;
                newRate = BigDecimal.ZERO;
            }

            if (!Integer.valueOf(newLevel).equals(agent.getLevel())) {
                agent.setLevel(newLevel);
                agent.setCommissionRate(newRate);
                agentMapper.updateById(agent);
                updated++;
                log.info("代理 userId={} 等级变更为 V{}", agent.getUserId(), newLevel);
            }
        }

        log.info("代理等级判定完成，共更新 {} 个代理", updated);
    }
}
