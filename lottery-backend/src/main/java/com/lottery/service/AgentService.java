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

        // 获取旗下玩家
        List<User> players = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getAgentId, userId)
        );
        Map<Long, User> playerMap = players.stream().collect(Collectors.toMap(User::getId, u -> u));
        List<Long> playerIds = new ArrayList<>(playerMap.keySet());

        List<Map<String, Object>> monthlyList = new ArrayList<>();
        BigDecimal allTimeTotal = BigDecimal.ZERO;
        int startYear = 0;
        BigDecimal rate = agent.getCommissionRate() != null ? agent.getCommissionRate() : BigDecimal.ZERO;

        if (!playerIds.isEmpty()) {
            LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<RechargeOrder>()
                    .in(RechargeOrder::getUserId, playerIds)
                    .eq(RechargeOrder::getStatus, "done");

            List<RechargeOrder> orders = rechargeOrderMapper.selectList(wrapper);

            // 使用数据库中累计的佣金（历史汇率准确）
            allTimeTotal = agent.getTotalCommission() != null ? agent.getTotalCommission() : BigDecimal.ZERO;

            // 找到最早有数据的年份
            for (RechargeOrder order : orders) {
                if (order.getCreatedAt() != null) {
                    int y = order.getCreatedAt().getYear();
                    if (startYear == 0 || y < startYear) startYear = y;
                }
            }

            // 筛选当年订单，按月 → 按玩家分组
            Map<Integer, Map<Long, List<RechargeOrder>>> monthPlayerOrders = new TreeMap<>();
            for (RechargeOrder order : orders) {
                if (order.getCreatedAt() == null || order.getAmount() == null) continue;
                if (order.getCreatedAt().getYear() != year) continue;
                int m = order.getCreatedAt().getMonthValue();
                if (month != null && month > 0 && m != month) continue;
                monthPlayerOrders
                        .computeIfAbsent(m, k -> new LinkedHashMap<>())
                        .computeIfAbsent(order.getUserId(), k -> new ArrayList<>())
                        .add(order);
            }

            for (Map.Entry<Integer, Map<Long, List<RechargeOrder>>> monthEntry : monthPlayerOrders.entrySet()) {
                double monthRecharge = 0;
                List<Map<String, Object>> details = new ArrayList<>();

                for (Map.Entry<Long, List<RechargeOrder>> playerEntry : monthEntry.getValue().entrySet()) {
                    Long playerId = playerEntry.getKey();
                    List<RechargeOrder> playerOrders = playerEntry.getValue();
                    User player = playerMap.get(playerId);

                    double playerRecharge = 0;
                    List<Map<String, Object>> orderList = new ArrayList<>();
                    for (RechargeOrder o : playerOrders) {
                        playerRecharge += o.getAmount();
                        Map<String, Object> orderItem = new LinkedHashMap<>();
                        orderItem.put("orderNo", o.getOrderNo());
                        orderItem.put("tokens", o.getTokens());
                        orderItem.put("amount", o.getAmount());
                        orderItem.put("commission", BigDecimal.valueOf(o.getAmount())
                                .multiply(rate)
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                        orderItem.put("createdAt", o.getCreatedAt());
                        orderList.add(orderItem);
                    }
                    monthRecharge += playerRecharge;

                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("playerId", playerId);
                    detail.put("nickname", player != null ? player.getNickname() : "未知");
                    detail.put("rechargeAmount", playerRecharge);
                    detail.put("commission", BigDecimal.valueOf(playerRecharge)
                            .multiply(rate)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                    detail.put("orders", orderList);
                    details.add(detail);
                }

                Map<String, Object> monthItem = new LinkedHashMap<>();
                monthItem.put("month", monthEntry.getKey());
                monthItem.put("rechargeAmount", monthRecharge);
                monthItem.put("commission", BigDecimal.valueOf(monthRecharge)
                        .multiply(rate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                monthItem.put("details", details);
                monthlyList.add(monthItem);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", allTimeTotal);
        result.put("commissionRate", rate);
        result.put("level", agent.getLevel());
        result.put("startYear", startYear > 0 ? startYear : year);
        result.put("list", monthlyList);
        return result;
    }
}
