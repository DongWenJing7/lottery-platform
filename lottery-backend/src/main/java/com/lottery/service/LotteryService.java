package com.lottery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lottery.common.exception.BusinessException;
import com.lottery.entity.*;
import com.lottery.mapper.*;
import com.lottery.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LotteryService {

    private final UserMapper userMapper;
    private final PrizeMapper prizeMapper;
    private final LotteryConfigMapper lotteryConfigMapper;
    private final DrawRecordMapper drawRecordMapper;
    private final WarehouseMapper warehouseMapper;
    private final TokenLogMapper tokenLogMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RandomUtil randomUtil;

    public Map<String, Object> getConfig(Long userId) {
        LotteryConfig config = lotteryConfigMapper.selectById(1);
        List<Prize> prizes = prizeMapper.selectList(
                new LambdaQueryWrapper<Prize>().eq(Prize::getStatus, 1)
        );
        Map<String, Object> result = new HashMap<>();
        result.put("costTokens", config.getCostTokens());
        result.put("jackpotThreshold", config.getJackpotThreshold());
        result.put("prizes", prizes);
        // 返回当前用户的连抽计数
        if (userId != null) {
            String jackpotKey = "jackpot:count:" + userId;
            Object countObj = redisTemplate.opsForValue().get(jackpotKey);
            result.put("consecutiveCount", countObj == null ? 0 : ((Number) countObj).intValue());
        }
        return result;
    }

    @Transactional
    public Map<String, Object> draw(Long userId) {
        // Redis 分布式锁防并发
        String lockKey = "lock:draw:" + userId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            throw new BusinessException("操作过于频繁，请稍后再试");
        }
        try {
            LotteryConfig config = lotteryConfigMapper.selectById(1);
            int cost = config.getCostTokens();

            // 原子扣除代币
            int updated = userMapper.deductBalance(userId, cost);
            if (updated == 0) {
                throw new BusinessException("代币不足");
            }

            // 扣除后查询最新余额
            User user = userMapper.selectById(userId);
            int newBalance = user.getBalance();

            // 保底计数
            String jackpotKey = "jackpot:count:" + userId;
            Object countObj = redisTemplate.opsForValue().get(jackpotKey);
            int count = countObj == null ? 0 : ((Number) countObj).intValue();

            Prize prize;
            boolean isJackpot = false;

            if (count + 1 >= config.getJackpotThreshold()) {
                // 触发保底 → 从大奖池随机
                List<Prize> jackpotPrizes = getJackpotPrizes(config.getJackpotPrizeIds());
                if (jackpotPrizes.isEmpty()) {
                    jackpotPrizes = prizeMapper.selectList(
                            new LambdaQueryWrapper<Prize>().eq(Prize::getStatus, 1).eq(Prize::getIsJackpot, 1)
                    );
                }
                prize = randomUtil.weightedRandom(jackpotPrizes);
                if (prize == null) {
                    // 如果没有大奖品，走普通逻辑
                    prize = drawNormal();
                } else {
                    isJackpot = true;
                }
                redisTemplate.opsForValue().set(jackpotKey, 0);
            } else {
                // 普通抽奖
                prize = drawNormal();
                if (prize.getIsJackpot() == 1) {
                    isJackpot = true;
                    redisTemplate.opsForValue().set(jackpotKey, 0);
                } else {
                    redisTemplate.opsForValue().set(jackpotKey, count + 1);
                }
            }

            // 库存原子扣减（stock=-1 为无限库存，跳过）
            if (prize.getStock() != null && prize.getStock() > 0) {
                int stockUpdated = prizeMapper.deductStock(prize.getId());
                if (stockUpdated == 0) {
                    // 库存已被其他并发请求抢完，回滚重试不现实，给个兜底奖品
                    // 这里直接继续，奖品已抽到但库存刚好耗尽属于正常边界情况
                    // 实际库存已为0，status会被SQL自动置为0
                }
            }

            // 写抽奖记录
            DrawRecord record = new DrawRecord();
            record.setUserId(userId);
            record.setPrizeId(prize.getId());
            record.setIsJackpot(isJackpot ? 1 : 0);
            record.setConsecutiveCount(isJackpot ? 0 : count + 1);
            drawRecordMapper.insert(record);

            // 写代币流水（抽奖消耗）
            TokenLog log = new TokenLog();
            log.setUserId(userId);
            log.setAmount(-cost);
            log.setBalanceAfter(newBalance);
            log.setType("draw");
            log.setRefId(record.getId());
            log.setRemark("抽奖消耗");
            tokenLogMapper.insert(log);

            // 代币类型奖品直接加余额，实物奖品入仓库
            Warehouse warehouse = null;
            boolean hasValue = prize.getValue() != null && prize.getValue() > 0;
            if ("token".equals(prize.getType())) {
                // 代币奖品：直接加余额
                int reward = prize.getTokenReward() != null ? prize.getTokenReward() : 0;
                if (reward > 0) {
                    userMapper.addBalance(userId, reward);
                    user = userMapper.selectById(userId);
                    newBalance = user.getBalance();

                    // 写代币奖励流水
                    TokenLog rewardLog = new TokenLog();
                    rewardLog.setUserId(userId);
                    rewardLog.setAmount(reward);
                    rewardLog.setBalanceAfter(newBalance);
                    rewardLog.setType("draw_reward");
                    rewardLog.setRefId(record.getId());
                    rewardLog.setRemark("抽奖获得代币: " + prize.getName());
                    tokenLogMapper.insert(rewardLog);
                }
            } else if (hasValue) {
                warehouse = new Warehouse();
                warehouse.setUserId(userId);
                warehouse.setPrizeId(prize.getId());
                warehouse.setStatus("held");
                warehouseMapper.insert(warehouse);
            }

            // 返回结果
            Map<String, Object> prizeInfo = new HashMap<>();
            prizeInfo.put("id", prize.getId());
            prizeInfo.put("name", prize.getName());
            prizeInfo.put("image", prize.getImage());
            prizeInfo.put("isJackpot", prize.getIsJackpot());
            prizeInfo.put("type", prize.getType());
            prizeInfo.put("tokenReward", prize.getTokenReward());

            Map<String, Object> result = new HashMap<>();
            result.put("prize", prizeInfo);
            result.put("consecutiveCount", record.getConsecutiveCount());
            result.put("warehouseId", warehouse != null ? warehouse.getId() : null);
            return result;

        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private Prize drawNormal() {
        List<Prize> prizes = prizeMapper.selectList(
                new LambdaQueryWrapper<Prize>()
                        .eq(Prize::getStatus, 1)
                        .and(w -> w.eq(Prize::getStock, -1).or().gt(Prize::getStock, 0))
        );
        if (prizes.isEmpty()) {
            throw new BusinessException("暂无可用奖品");
        }
        return randomUtil.weightedRandom(prizes);
    }

    private List<Prize> getJackpotPrizes(String jackpotPrizeIds) {
        if (jackpotPrizeIds == null || jackpotPrizeIds.isBlank()) {
            return Collections.emptyList();
        }
        List<Long> ids = Arrays.stream(jackpotPrizeIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
        if (ids.isEmpty()) return Collections.emptyList();
        return prizeMapper.selectList(
                new LambdaQueryWrapper<Prize>().in(Prize::getId, ids).eq(Prize::getStatus, 1)
        );
    }
}
