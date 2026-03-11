package com.lottery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lottery.common.PageResult;
import com.lottery.common.exception.BusinessException;
import com.lottery.entity.*;
import com.lottery.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final AgentMapper agentMapper;
    private final TokenLogMapper tokenLogMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final DeliveryOrderMapper deliveryOrderMapper;
    private final DrawRecordMapper drawRecordMapper;
    private final PrizeMapper prizeMapper;
    private final WarehouseMapper warehouseMapper;
    private final MarketMapper marketMapper;
    private final LotteryConfigMapper lotteryConfigMapper;

    // ==================== 数据概览 ====================

    public Map<String, Object> getDashboard() {
        long totalUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, "player")
        );
        // 总充值代币
        List<RechargeOrder> doneOrders = rechargeOrderMapper.selectList(
                new LambdaQueryWrapper<RechargeOrder>().eq(RechargeOrder::getStatus, "done")
        );
        int totalRecharge = doneOrders.stream().mapToInt(RechargeOrder::getTokens).sum();

        // 今日抽奖次数
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayDraw = drawRecordMapper.selectCount(
                new LambdaQueryWrapper<DrawRecord>().ge(DrawRecord::getCreatedAt, todayStart)
        );

        long pendingRecharge = rechargeOrderMapper.selectCount(
                new LambdaQueryWrapper<RechargeOrder>().eq(RechargeOrder::getStatus, "pending")
        );
        long pendingDelivery = deliveryOrderMapper.selectCount(
                new LambdaQueryWrapper<DeliveryOrder>().eq(DeliveryOrder::getStatus, "pending")
        );

        // 今日统计
        long todayNewUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, "player").ge(User::getCreatedAt, todayStart)
        );
        List<RechargeOrder> todayDoneOrders = rechargeOrderMapper.selectList(
                new LambdaQueryWrapper<RechargeOrder>()
                        .eq(RechargeOrder::getStatus, "done")
                        .ge(RechargeOrder::getCreatedAt, todayStart)
        );
        int todayRecharge = todayDoneOrders.stream().mapToInt(RechargeOrder::getTokens).sum();
        double todayRechargeAmount = todayDoneOrders.stream()
                .mapToDouble(o -> o.getAmount() != null ? o.getAmount() : 0).sum();
        long todayDelivery = deliveryOrderMapper.selectCount(
                new LambdaQueryWrapper<DeliveryOrder>().ge(DeliveryOrder::getCreatedAt, todayStart)
        );

        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", totalUsers);
        result.put("totalRecharge", totalRecharge);
        result.put("todayDraw", todayDraw);
        result.put("pendingRecharge", pendingRecharge);
        result.put("pendingDelivery", pendingDelivery);
        result.put("todayNewUsers", todayNewUsers);
        result.put("todayRecharge", todayRecharge);
        result.put("todayRechargeAmount", todayRechargeAmount);
        result.put("todayDelivery", todayDelivery);
        return result;
    }

    // ==================== 玩家管理 ====================

    public PageResult<Map<String, Object>> getPlayers(int page, int size, String keyword, Long agentId, String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .ne(User::getRole, "admin");
        if (role != null && !role.isEmpty()) {
            wrapper.eq(User::getRole, role);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getNickname, keyword).or().like(User::getUsername, keyword));
        }
        if (agentId != null) {
            wrapper.eq(User::getAgentId, agentId);
        }
        wrapper.orderByAsc(User::getId);
        Page<User> p = userMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量获取代理昵称
        Set<Long> agentIds = p.getRecords().stream()
                .map(User::getAgentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> agentNicknames = agentIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectBatchIds(agentIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getNickname));

        List<Map<String, Object>> list = p.getRecords().stream().map(u -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("username", u.getUsername());
            item.put("nickname", u.getNickname());
            item.put("balance", u.getBalance());
            item.put("role", u.getRole());
            item.put("status", u.getStatus());
            item.put("createdAt", u.getCreatedAt());
            item.put("agentNickname", u.getAgentId() != null ? agentNicknames.get(u.getAgentId()) : null);
            return item;
        }).collect(Collectors.toList());

        return PageResult.of(list, p.getTotal());
    }

    @Transactional
    public void updateTokens(Long userId, int amount, String remark, Long operatorId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        if (amount > 0) {
            userMapper.addBalance(userId, amount);
        } else {
            int updated = userMapper.deductBalance(userId, -amount);
            if (updated == 0) throw new BusinessException("代币不足");
        }

        User userAfter = userMapper.selectById(userId);
        TokenLog log = new TokenLog();
        log.setUserId(userId);
        log.setAmount(amount);
        log.setBalanceAfter(userAfter.getBalance());
        log.setType("manual");
        log.setRemark(remark != null ? remark : "管理员手动调整");
        tokenLogMapper.insert(log);
    }

    public void setStatus(Long userId, int status) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        user.setStatus(status);
        userMapper.updateById(user);
    }

    // ==================== 仓库管理 ====================

    public List<Map<String, Object>> getWarehouseItems(Long userId) {
        List<Warehouse> items = warehouseMapper.selectList(
                new LambdaQueryWrapper<Warehouse>()
                        .eq(Warehouse::getUserId, userId)
                        .in(Warehouse::getStatus, "held", "selling", "delivered")
                        .orderByDesc(Warehouse::getCreatedAt));
        Set<Long> prizeIds = items.stream().map(Warehouse::getPrizeId).collect(Collectors.toSet());
        Map<Long, Prize> prizeMap = prizeIds.isEmpty() ? Collections.emptyMap() :
                prizeMapper.selectBatchIds(prizeIds).stream().collect(Collectors.toMap(Prize::getId, p -> p));
        return items.stream().map(w -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", w.getId());
            map.put("prizeId", w.getPrizeId());
            map.put("status", w.getStatus());
            Prize prize = prizeMap.get(w.getPrizeId());
            if (prize != null) {
                Map<String, Object> pm = new HashMap<>();
                pm.put("name", prize.getName());
                pm.put("value", prize.getValue());
                map.put("prize", pm);
            }
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void addWarehouseItem(Long userId, Long prizeId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        Prize prize = prizeMapper.selectById(prizeId);
        if (prize == null) throw new BusinessException("奖品不存在");

        Warehouse item = new Warehouse();
        item.setUserId(userId);
        item.setPrizeId(prizeId);
        item.setStatus("held");
        warehouseMapper.insert(item);
    }

    @Transactional
    public void removeWarehouseItem(Long userId, Long itemId) {
        Warehouse item = warehouseMapper.selectById(itemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException("物品不存在");
        }
        // 联动下架市场
        if ("selling".equals(item.getStatus())) {
            Market market = marketMapper.selectOne(
                    new LambdaQueryWrapper<Market>()
                            .eq(Market::getWarehouseId, itemId)
                            .eq(Market::getStatus, "on")
            );
            if (market != null) {
                market.setStatus("off");
                marketMapper.updateById(market);
            }
        }
        warehouseMapper.deleteById(itemId);
    }

    // ==================== 奖品管理 ====================

    public PageResult<Prize> getPrizes(int page, int size) {
        Page<Prize> p = prizeMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Prize>().orderByDesc(Prize::getCreatedAt));
        return PageResult.of(p.getRecords(), p.getTotal());
    }

    public void createPrize(Prize prize) {
        prizeMapper.insert(prize);
    }

    public void updatePrize(Long id, Prize prize) {
        Prize existing = prizeMapper.selectById(id);
        if (existing == null) throw new BusinessException("奖品不存在");
        prize.setId(id);
        prizeMapper.updateById(prize);
    }

    public void deletePrize(Long id) {
        prizeMapper.deleteById(id);
    }

    // ==================== 抽奖配置 ====================

    public LotteryConfig getLotteryConfig() {
        return lotteryConfigMapper.selectById(1);
    }

    public void updateLotteryConfig(LotteryConfig config) {
        config.setId(1);
        lotteryConfigMapper.updateById(config);
    }

    // ==================== 充值管理 ====================

    public PageResult<Map<String, Object>> getRechargeOrders(int page, int size, String status) {
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<RechargeOrder>()
                .eq(status != null && !status.isEmpty(), RechargeOrder::getStatus, status)
                .orderByDesc(RechargeOrder::getCreatedAt);
        Page<RechargeOrder> p = rechargeOrderMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量获取用户
        Set<Long> userIds = p.getRecords().stream().map(RechargeOrder::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        List<Map<String, Object>> list = p.getRecords().stream().map(o -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", o.getId());
            item.put("orderNo", o.getOrderNo());
            item.put("tokens", o.getTokens());
            item.put("amount", o.getAmount());
            item.put("remark", o.getRemark());
            item.put("status", o.getStatus());
            item.put("rejectReason", o.getRejectReason());
            item.put("createdAt", o.getCreatedAt());
            User u = userMap.get(o.getUserId());
            if (u != null) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", u.getId());
                userInfo.put("nickname", u.getNickname());
                userInfo.put("username", u.getUsername());
                item.put("user", userInfo);
            }
            return item;
        }).collect(Collectors.toList());

        return PageResult.of(list, p.getTotal());
    }

    @Transactional
    public void confirmRecharge(Long id, Long operatorId) {
        RechargeOrder order = rechargeOrderMapper.selectById(id);
        if (order == null || !"pending".equals(order.getStatus())) {
            throw new BusinessException("订单不存在或已处理");
        }

        // 更新订单
        order.setStatus("done");
        order.setOperatorId(operatorId);
        rechargeOrderMapper.updateById(order);

        // 给用户加代币
        userMapper.addBalance(order.getUserId(), order.getTokens());
        User user = userMapper.selectById(order.getUserId());

        // 写流水
        TokenLog log = new TokenLog();
        log.setUserId(user.getId());
        log.setAmount(order.getTokens());
        log.setBalanceAfter(user.getBalance());
        log.setType("recharge");
        log.setRefId(order.getId());
        log.setRemark("充值到账");
        tokenLogMapper.insert(log);

        // 佣金逻辑
        if (user.getAgentId() != null) {
            Agent agent = agentMapper.findByUserId(user.getAgentId());
            if (agent != null) {
                // 先更新团队充值总量（null 安全）
                int teamRecharge = (agent.getTeamRecharge() != null ? agent.getTeamRecharge() : 0) + order.getTokens();
                agent.setTeamRecharge(teamRecharge);

                // 实时判定代理等级
                int teamCount = agent.getTeamCount() != null ? agent.getTeamCount() : 0;
                if (teamCount >= 50 || teamRecharge >= 5000) {
                    agent.setLevel(3);
                    agent.setCommissionRate(new BigDecimal("3.00"));
                } else if (teamCount >= 30 || teamRecharge >= 3000) {
                    agent.setLevel(2);
                    agent.setCommissionRate(new BigDecimal("2.00"));
                } else if (teamCount >= 10 || teamRecharge >= 1000) {
                    agent.setLevel(1);
                    agent.setCommissionRate(new BigDecimal("1.00"));
                } else {
                    agent.setLevel(0);
                    agent.setCommissionRate(BigDecimal.ZERO);
                }

                // 按最新佣金比例计算佣金
                BigDecimal rate = agent.getCommissionRate() != null ? agent.getCommissionRate() : BigDecimal.ZERO;
                if (rate.compareTo(BigDecimal.ZERO) > 0 && order.getAmount() != null) {
                    BigDecimal commission = BigDecimal.valueOf(order.getAmount())
                            .multiply(rate)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    BigDecimal totalCommission = agent.getTotalCommission() != null ? agent.getTotalCommission() : BigDecimal.ZERO;
                    agent.setTotalCommission(totalCommission.add(commission));
                }
                agentMapper.updateById(agent);
            }
        }
    }

    public void rejectRecharge(Long id, String reason) {
        RechargeOrder order = rechargeOrderMapper.selectById(id);
        if (order == null || !"pending".equals(order.getStatus())) {
            throw new BusinessException("订单不存在或已处理");
        }
        order.setStatus("rejected");
        order.setRejectReason(reason);
        rechargeOrderMapper.updateById(order);
    }

    // ==================== 发货管理 ====================

    public PageResult<Map<String, Object>> getDeliveryOrders(int page, int size, String status) {
        LambdaQueryWrapper<DeliveryOrder> wrapper = new LambdaQueryWrapper<DeliveryOrder>()
                .eq(status != null && !status.isEmpty(), DeliveryOrder::getStatus, status)
                .orderByDesc(DeliveryOrder::getCreatedAt);
        Page<DeliveryOrder> p = deliveryOrderMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量获取用户和仓库/奖品信息
        Set<Long> userIds = p.getRecords().stream().map(DeliveryOrder::getUserId).collect(Collectors.toSet());
        Set<Long> warehouseIds = p.getRecords().stream().map(DeliveryOrder::getWarehouseId).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Warehouse> warehouseMap = warehouseIds.isEmpty() ? Collections.emptyMap() :
                warehouseMapper.selectBatchIds(warehouseIds).stream().collect(Collectors.toMap(Warehouse::getId, w -> w));
        Set<Long> prizeIds = warehouseMap.values().stream().map(Warehouse::getPrizeId).collect(Collectors.toSet());
        Map<Long, Prize> prizeMap = prizeIds.isEmpty() ? Collections.emptyMap() :
                prizeMapper.selectBatchIds(prizeIds).stream().collect(Collectors.toMap(Prize::getId, pr -> pr));

        List<Map<String, Object>> list = p.getRecords().stream().map(o -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", o.getId());
            item.put("orderNo", o.getOrderNo());
            item.put("status", o.getStatus());
            item.put("receiverName", o.getReceiverName());
            item.put("receiverPhone", o.getReceiverPhone());
            item.put("receiverAddress", o.getReceiverAddress());
            item.put("createdAt", o.getCreatedAt());
            item.put("shippedAt", o.getShippedAt());
            item.put("doneAt", o.getDoneAt());
            User u = userMap.get(o.getUserId());
            if (u != null) {
                item.put("user", Map.of("id", u.getId(), "nickname", u.getNickname()));
            }
            Warehouse wh = warehouseMap.get(o.getWarehouseId());
            if (wh != null) {
                item.put("prize", prizeMap.get(wh.getPrizeId()));
            }
            return item;
        }).collect(Collectors.toList());

        return PageResult.of(list, p.getTotal());
    }

    public void shipOrder(Long id) {
        DeliveryOrder order = deliveryOrderMapper.selectById(id);
        if (order == null || !"pending".equals(order.getStatus())) {
            throw new BusinessException("订单不存在或状态异常");
        }
        order.setStatus("shipped");
        order.setShippedAt(LocalDateTime.now());
        deliveryOrderMapper.updateById(order);
    }

    public void completeOrder(Long id) {
        DeliveryOrder order = deliveryOrderMapper.selectById(id);
        if (order == null || !"shipped".equals(order.getStatus())) {
            throw new BusinessException("订单不存在或状态异常");
        }
        order.setStatus("done");
        order.setDoneAt(LocalDateTime.now());
        deliveryOrderMapper.updateById(order);
    }

    // ==================== 代理管理 ====================

    public PageResult<Map<String, Object>> getAgents(int page, int size) {
        Page<Agent> p = agentMapper.selectPage(new Page<>(page, size), null);
        Set<Long> userIds = p.getRecords().stream().map(Agent::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        List<Map<String, Object>> list = p.getRecords().stream().map(a -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", a.getId());
            item.put("level", a.getLevel());
            item.put("teamCount", a.getTeamCount());
            item.put("teamRecharge", a.getTeamRecharge());
            item.put("totalCommission", a.getTotalCommission());
            item.put("commissionRate", a.getCommissionRate());
            User u = userMap.get(a.getUserId());
            if (u != null) {
                item.put("nickname", u.getNickname());
                item.put("userId", u.getId());
            }
            return item;
        }).collect(Collectors.toList());

        return PageResult.of(list, p.getTotal());
    }

    // ==================== 市场管理 ====================

    public PageResult<Map<String, Object>> getMarket(int page, int size, String status) {
        LambdaQueryWrapper<Market> wrapper = new LambdaQueryWrapper<Market>()
                .eq(status != null && !status.isEmpty(), Market::getStatus, status)
                .orderByDesc(Market::getCreatedAt);
        Page<Market> p = marketMapper.selectPage(new Page<>(page, size), wrapper);

        Set<Long> warehouseIds = p.getRecords().stream().map(Market::getWarehouseId).collect(Collectors.toSet());
        Set<Long> sellerIds = p.getRecords().stream().map(Market::getSellerId).collect(Collectors.toSet());
        Set<Long> buyerIds = p.getRecords().stream().map(Market::getBuyerId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, Warehouse> warehouseMap = warehouseIds.isEmpty() ? Collections.emptyMap() :
                warehouseMapper.selectBatchIds(warehouseIds).stream().collect(Collectors.toMap(Warehouse::getId, w -> w));
        Set<Long> prizeIds = warehouseMap.values().stream().map(Warehouse::getPrizeId).collect(Collectors.toSet());
        Map<Long, Prize> prizeMap = prizeIds.isEmpty() ? Collections.emptyMap() :
                prizeMapper.selectBatchIds(prizeIds).stream().collect(Collectors.toMap(Prize::getId, pr -> pr));
        Set<Long> allUserIds = new HashSet<>();
        allUserIds.addAll(sellerIds);
        allUserIds.addAll(buyerIds);
        Map<Long, User> userMap = allUserIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectBatchIds(allUserIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        List<Map<String, Object>> list = p.getRecords().stream().map(m -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", m.getId());
            item.put("price", m.getPrice());
            item.put("status", m.getStatus());
            item.put("createdAt", m.getCreatedAt());
            Warehouse wh = warehouseMap.get(m.getWarehouseId());
            if (wh != null) {
                item.put("prize", prizeMap.get(wh.getPrizeId()));
            }
            User seller = userMap.get(m.getSellerId());
            if (seller != null) {
                item.put("seller", Map.of("id", seller.getId(), "nickname", seller.getNickname()));
            }
            if (m.getBuyerId() != null) {
                User buyer = userMap.get(m.getBuyerId());
                if (buyer != null) {
                    item.put("buyer", Map.of("id", buyer.getId(), "nickname", buyer.getNickname()));
                }
            }
            return item;
        }).collect(Collectors.toList());

        return PageResult.of(list, p.getTotal());
    }

    @Transactional
    public void removeMarketItem(Long id) {
        Market market = marketMapper.selectById(id);
        if (market == null) throw new BusinessException("市场商品不存在");

        if ("on".equals(market.getStatus())) {
            market.setStatus("off");
            marketMapper.updateById(market);

            // 恢复仓库状态
            Warehouse item = warehouseMapper.selectById(market.getWarehouseId());
            if (item != null) {
                item.setStatus("held");
                item.setSellPrice(null);
                warehouseMapper.updateById(item);
            }
        }
    }
}
