package com.lottery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lottery.common.PageResult;
import com.lottery.common.exception.BusinessException;
import com.lottery.entity.*;
import com.lottery.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final UserMapper userMapper;
    private final TokenLogMapper tokenLogMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final WarehouseMapper warehouseMapper;
    private final PrizeMapper prizeMapper;
    private final MarketMapper marketMapper;
    private final DeliveryOrderMapper deliveryOrderMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    // ==================== 个人信息 ====================

    public Map<String, Object> getInfo(Long userId) {
        User user = userMapper.selectById(userId);
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("nickname", user.getNickname());
        info.put("avatar", user.getAvatar());
        info.put("address", user.getAddress());
        info.put("balance", user.getBalance());
        info.put("username", user.getUsername());
        if (user.getAgentId() != null) {
            User agent = userMapper.selectById(user.getAgentId());
            info.put("agentNickname", agent != null ? agent.getNickname() : null);
        }
        return info;
    }

    public void bindPhone(Long userId, String password, String phone) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        if (!phone.matches("^1\\d{10}$")) {
            throw new BusinessException("请输入正确的11位手机号");
        }
        User existing = userMapper.findByUsername(phone);
        if (existing != null && !existing.getId().equals(userId)) {
            throw new BusinessException("该手机号已被使用");
        }
        user.setUsername(phone);
        userMapper.updateById(user);
    }

    public void updateProfile(Long userId, String nickname, String avatar, String address) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (nickname != null && !nickname.isEmpty()) {
            user.setNickname(nickname);
        }
        if (avatar != null) {
            user.setAvatar(avatar);
        }
        if (address != null) {
            user.setAddress(address);
        }
        userMapper.updateById(user);
    }

    // ==================== 代币流水 ====================

    public PageResult<TokenLog> getLogs(Long userId, int page, int size, String type) {
        LambdaQueryWrapper<TokenLog> wrapper = new LambdaQueryWrapper<TokenLog>()
                .eq(TokenLog::getUserId, userId)
                .eq(type != null && !type.isEmpty(), TokenLog::getType, type)
                .orderByDesc(TokenLog::getCreatedAt);
        Page<TokenLog> p = tokenLogMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords(), p.getTotal());
    }

    // ==================== 充值 ====================

    public PageResult<RechargeOrder> getRechargeOrders(Long userId, int page, int size) {
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<RechargeOrder>()
                .eq(RechargeOrder::getUserId, userId)
                .orderByDesc(RechargeOrder::getCreatedAt);
        Page<RechargeOrder> p = rechargeOrderMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords(), p.getTotal());
    }

    // ==================== 提交充值申请 ====================

    public Map<String, Object> submitRecharge(Long userId, int tokens, double amount, String remark) {
        if (tokens <= 0) {
            throw new BusinessException("充值代币数量必须大于0");
        }
        RechargeOrder order = new RechargeOrder();
        order.setOrderNo(generateOrderNo("R"));
        order.setUserId(userId);
        order.setTokens(tokens);
        order.setAmount(amount);
        order.setRemark(remark);
        order.setStatus("pending");
        rechargeOrderMapper.insert(order);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("orderNo", order.getOrderNo());
        return result;
    }

    // ==================== 仓库 ====================

    public PageResult<Map<String, Object>> getWarehouse(Long userId, int page, int size, String status) {
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<Warehouse>()
                .eq(Warehouse::getUserId, userId)
                .eq(status != null && !status.isEmpty(), Warehouse::getStatus, status)
                .orderByDesc(Warehouse::getCreatedAt);
        Page<Warehouse> p = warehouseMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量获取奖品信息
        List<Long> prizeIds = p.getRecords().stream().map(Warehouse::getPrizeId).distinct().collect(Collectors.toList());
        Map<Long, Prize> prizeMap = prizeIds.isEmpty() ? Collections.emptyMap() :
                prizeMapper.selectBatchIds(prizeIds).stream().collect(Collectors.toMap(Prize::getId, pr -> pr));

        List<Map<String, Object>> list = p.getRecords().stream()
                .filter(w -> {
                    Prize pr = prizeMap.get(w.getPrizeId());
                    return pr != null && pr.getValue() != null && pr.getValue() > 0;
                })
                .map(w -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", w.getId());
                    item.put("prize", prizeMap.get(w.getPrizeId()));
                    item.put("status", w.getStatus());
                    item.put("sellPrice", w.getSellPrice());
                    item.put("createdAt", w.getCreatedAt());
                    return item;
                }).collect(Collectors.toList());

        return PageResult.of(list, p.getTotal());
    }

    @Transactional
    public void deleteItem(Long userId, Long warehouseId) {
        Warehouse item = warehouseMapper.selectById(warehouseId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException("物品不存在");
        }
        if ("delivered".equals(item.getStatus())) {
            throw new BusinessException("已发货的物品不可删除");
        }
        // 如果正在挂售，先下架市场
        if ("selling".equals(item.getStatus())) {
            Market market = marketMapper.selectOne(
                    new LambdaQueryWrapper<Market>()
                            .eq(Market::getWarehouseId, warehouseId)
                            .eq(Market::getStatus, "on")
            );
            if (market != null) {
                market.setStatus("off");
                marketMapper.updateById(market);
            }
        }
        warehouseMapper.deleteById(warehouseId);
    }

    // ==================== 挂售/取消 ====================

    @Transactional
    public void listForSale(Long userId, Long warehouseId, int price) {
        Warehouse item = warehouseMapper.selectById(warehouseId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException("物品不存在");
        }
        if (!"held".equals(item.getStatus())) {
            throw new BusinessException("当前状态不可挂售");
        }

        item.setStatus("selling");
        item.setSellPrice(price);
        warehouseMapper.updateById(item);

        Market market = new Market();
        market.setWarehouseId(warehouseId);
        market.setSellerId(userId);
        market.setPrice(price);
        market.setStatus("on");
        marketMapper.insert(market);
    }

    @Transactional
    public void unsell(Long userId, Long warehouseId) {
        Warehouse item = warehouseMapper.selectById(warehouseId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException("物品不存在");
        }
        if (!"selling".equals(item.getStatus())) {
            throw new BusinessException("当前未在售");
        }

        item.setStatus("held");
        item.setSellPrice(null);
        warehouseMapper.updateById(item);

        // 下架市场
        Market market = marketMapper.selectOne(
                new LambdaQueryWrapper<Market>()
                        .eq(Market::getWarehouseId, warehouseId)
                        .eq(Market::getStatus, "on")
        );
        if (market != null) {
            market.setStatus("off");
            marketMapper.updateById(market);
        }
    }

    // ==================== 申请发货 ====================

    @Transactional
    public Map<String, Object> requestDelivery(Long userId, Long warehouseId,
                                                String receiverName, String receiverPhone, String receiverAddress) {
        Warehouse item = warehouseMapper.selectById(warehouseId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException("物品不存在");
        }
        if (!"held".equals(item.getStatus())) {
            throw new BusinessException("当前状态不可发货");
        }

        item.setStatus("delivered");
        warehouseMapper.updateById(item);

        DeliveryOrder order = new DeliveryOrder();
        order.setOrderNo(generateOrderNo("D"));
        order.setUserId(userId);
        order.setWarehouseId(warehouseId);
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setReceiverAddress(receiverAddress);
        order.setStatus("pending");
        deliveryOrderMapper.insert(order);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("orderNo", order.getOrderNo());
        return result;
    }

    // ==================== 转售市场 ====================

    public PageResult<Map<String, Object>> getMarket(int page, int size, String keyword) {
        // 查在售的市场商品
        LambdaQueryWrapper<Market> wrapper = new LambdaQueryWrapper<Market>()
                .eq(Market::getStatus, "on")
                .orderByDesc(Market::getCreatedAt);
        Page<Market> p = marketMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量获取仓库、奖品、卖家信息
        List<Market> records = p.getRecords();
        Set<Long> warehouseIds = records.stream().map(Market::getWarehouseId).collect(Collectors.toSet());
        Set<Long> sellerIds = records.stream().map(Market::getSellerId).collect(Collectors.toSet());

        Map<Long, Warehouse> warehouseMap = warehouseIds.isEmpty() ? Collections.emptyMap() :
                warehouseMapper.selectBatchIds(warehouseIds).stream().collect(Collectors.toMap(Warehouse::getId, w -> w));

        Set<Long> prizeIds = warehouseMap.values().stream().map(Warehouse::getPrizeId).collect(Collectors.toSet());
        Map<Long, Prize> prizeMap = prizeIds.isEmpty() ? Collections.emptyMap() :
                prizeMapper.selectBatchIds(prizeIds).stream().collect(Collectors.toMap(Prize::getId, pr -> pr));

        Map<Long, User> sellerMap = sellerIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectBatchIds(sellerIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        List<Map<String, Object>> list = records.stream().map(m -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", m.getId());
            item.put("price", m.getPrice());
            Warehouse wh = warehouseMap.get(m.getWarehouseId());
            if (wh != null) {
                item.put("prize", prizeMap.get(wh.getPrizeId()));
            }
            User seller = sellerMap.get(m.getSellerId());
            if (seller != null) {
                Map<String, Object> sellerInfo = new HashMap<>();
                sellerInfo.put("id", seller.getId());
                sellerInfo.put("nickname", seller.getNickname());
                item.put("seller", sellerInfo);
            }
            return item;
        }).collect(Collectors.toList());

        // keyword 过滤（按奖品名称）
        if (keyword != null && !keyword.isEmpty()) {
            list = list.stream().filter(item -> {
                Object prize = item.get("prize");
                if (prize instanceof Prize) {
                    return ((Prize) prize).getName().contains(keyword);
                }
                return false;
            }).collect(Collectors.toList());
        }

        return PageResult.of(list, p.getTotal());
    }

    @Transactional
    public Map<String, Object> buyItem(Long userId, Long marketId) {
        Market market = marketMapper.selectById(marketId);
        if (market == null || !"on".equals(market.getStatus())) {
            throw new BusinessException("商品不存在或已下架");
        }
        if (market.getSellerId().equals(userId)) {
            throw new BusinessException("不能购买自己的商品");
        }

        int price = market.getPrice();

        // 原子抢购：防止并发双卖
        int marketUpdated = marketMapper.atomicBuy(marketId, userId);
        if (marketUpdated == 0) {
            throw new BusinessException("商品已被抢走了");
        }

        // 扣买家代币
        int updated = userMapper.deductBalance(userId, price);
        if (updated == 0) {
            throw new BusinessException("代币不足");
        }

        // 加卖家代币
        userMapper.addBalance(market.getSellerId(), price);

        // 转移仓库物品
        Warehouse item = warehouseMapper.selectById(market.getWarehouseId());
        item.setUserId(userId);
        item.setStatus("held");
        item.setSellPrice(null);
        warehouseMapper.updateById(item);

        // 写流水 - 买家
        User buyer = userMapper.selectById(userId);
        TokenLog buyLog = new TokenLog();
        buyLog.setUserId(userId);
        buyLog.setAmount(-price);
        buyLog.setBalanceAfter(buyer.getBalance());
        buyLog.setType("buy");
        buyLog.setRefId(market.getId());
        buyLog.setRemark("购买市场商品");
        tokenLogMapper.insert(buyLog);

        // 写流水 - 卖家
        User seller = userMapper.selectById(market.getSellerId());
        TokenLog sellLog = new TokenLog();
        sellLog.setUserId(market.getSellerId());
        sellLog.setAmount(price);
        sellLog.setBalanceAfter(seller.getBalance());
        sellLog.setType("sell");
        sellLog.setRefId(market.getId());
        sellLog.setRemark("出售市场商品");
        tokenLogMapper.insert(sellLog);

        Map<String, Object> result = new HashMap<>();
        result.put("warehouseId", item.getId());
        result.put("balanceAfter", buyer.getBalance());
        return result;
    }

    // ==================== 发货订单 ====================

    public PageResult<Map<String, Object>> getOrders(Long userId, int page, int size, String status) {
        LambdaQueryWrapper<DeliveryOrder> wrapper = new LambdaQueryWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getUserId, userId)
                .eq(status != null && !status.isEmpty(), DeliveryOrder::getStatus, status)
                .orderByDesc(DeliveryOrder::getCreatedAt);
        Page<DeliveryOrder> p = deliveryOrderMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量获取仓库和奖品
        Set<Long> warehouseIds = p.getRecords().stream().map(DeliveryOrder::getWarehouseId).collect(Collectors.toSet());
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
            Warehouse wh = warehouseMap.get(o.getWarehouseId());
            if (wh != null) {
                item.put("prize", prizeMap.get(wh.getPrizeId()));
            }
            return item;
        }).collect(Collectors.toList());

        return PageResult.of(list, p.getTotal());
    }

    // ==================== 工具 ====================

    private String generateOrderNo(String prefix) {
        return prefix + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
    }
}
