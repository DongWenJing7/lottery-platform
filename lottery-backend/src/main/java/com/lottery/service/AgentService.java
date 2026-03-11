package com.lottery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lottery.common.PageResult;
import com.lottery.common.exception.BusinessException;
import com.lottery.entity.Agent;
import com.lottery.entity.RechargeOrder;
import com.lottery.entity.User;
import com.lottery.mapper.AgentMapper;
import com.lottery.mapper.RechargeOrderMapper;
import com.lottery.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentMapper agentMapper;
    private final UserMapper userMapper;
    private final RechargeOrderMapper rechargeOrderMapper;

    public Map<String, Object> getDashboard(Long userId) {
        Agent agent = agentMapper.findByUserId(userId);
        if (agent == null) {
            throw new BusinessException("代理信息不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("teamCount", agent.getTeamCount());
        result.put("teamRecharge", agent.getTeamRecharge());
        result.put("totalCommission", agent.getTotalCommission());
        result.put("level", agent.getLevel());
        result.put("commissionRate", agent.getCommissionRate());
        result.put("inviteCode", agent.getInviteCode());
        return result;
    }

    public PageResult<Map<String, Object>> getPlayers(Long userId, int page, int size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getAgentId, userId)
                .eq(User::getRole, "player");
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getNickname, keyword).or().like(User::getUsername, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt);
        Page<User> p = userMapper.selectPage(new Page<>(page, size), wrapper);

        // 计算每个玩家的充值总量
        List<Map<String, Object>> list = p.getRecords().stream().map(u -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("nickname", u.getNickname());
            item.put("balance", u.getBalance());
            item.put("createdAt", u.getCreatedAt());

            // 该玩家确认的充值总量
            LambdaQueryWrapper<RechargeOrder> rw = new LambdaQueryWrapper<RechargeOrder>()
                    .eq(RechargeOrder::getUserId, u.getId())
                    .eq(RechargeOrder::getStatus, "done");
            List<RechargeOrder> orders = rechargeOrderMapper.selectList(rw);
            int totalRecharge = orders.stream().mapToInt(RechargeOrder::getTokens).sum();
            item.put("totalRecharge", totalRecharge);

            return item;
        }).collect(Collectors.toList());

        return PageResult.of(list, p.getTotal());
    }

    public Map<String, Object> getCommission(Long userId, Integer year, Integer month) {
        Agent agent = agentMapper.findByUserId(userId);
        if (agent == null) {
            throw new BusinessException("代理信息不存在");
        }

        // 获取旗下玩家 ID
        List<User> players = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getAgentId, userId)
        );
        List<Long> playerIds = players.stream().map(User::getId).collect(Collectors.toList());

        List<Map<String, Object>> monthlyList = new ArrayList<>();
        BigDecimal allTimeTotal = BigDecimal.ZERO;

        if (!playerIds.isEmpty()) {
            LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<RechargeOrder>()
                    .in(RechargeOrder::getUserId, playerIds)
                    .eq(RechargeOrder::getStatus, "done");

            List<RechargeOrder> orders = rechargeOrderMapper.selectList(wrapper);
            BigDecimal rate = agent.getCommissionRate();

            // 按月分组（选中年份），用于月度明细
            Map<Integer, Double> monthlyAmount = new TreeMap<>();
            // 全量累计（所有年份），用于累计佣金
            double allTimeAmount = 0;

            for (RechargeOrder order : orders) {
                if (order.getCreatedAt() == null || order.getAmount() == null) continue;
                allTimeAmount += order.getAmount();
                if (order.getCreatedAt().getYear() == year) {
                    int m = order.getCreatedAt().getMonthValue();
                    if (month != null && month > 0 && m != month) continue;
                    monthlyAmount.merge(m, order.getAmount(), Double::sum);
                }
            }

            for (Map.Entry<Integer, Double> entry : monthlyAmount.entrySet()) {
                Map<String, Object> item = new HashMap<>();
                item.put("month", entry.getKey());
                BigDecimal commission = BigDecimal.valueOf(entry.getValue())
                        .multiply(rate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                item.put("amount", commission);
                monthlyList.add(item);
            }

            allTimeTotal = BigDecimal.valueOf(allTimeAmount)
                    .multiply(rate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", monthlyList);
        result.put("total", allTimeTotal);
        return result;
    }
}
