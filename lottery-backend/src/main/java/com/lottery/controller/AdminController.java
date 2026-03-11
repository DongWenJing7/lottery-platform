package com.lottery.controller;

import com.lottery.common.Result;
import com.lottery.entity.Agent;
import com.lottery.entity.LotteryConfig;
import com.lottery.entity.Prize;
import com.lottery.entity.User;
import com.lottery.interceptor.AuthInterceptor;
import com.lottery.mapper.AgentMapper;
import com.lottery.mapper.UserMapper;
import com.lottery.service.AdminService;
import com.lottery.util.RandomUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserMapper userMapper;
    private final AgentMapper agentMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    // ==================== 新增用户 ====================

    @PostMapping("/players")
    public Result<?> createUser(@RequestBody CreateUserDTO dto) {
        if (userMapper.findByUsername(dto.getUsername()) != null) {
            return Result.error(400, "用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setRole(dto.getRole() != null ? dto.getRole() : "player");
        user.setBalance(0);
        user.setStatus(1);
        userMapper.insert(user);

        // 如果是代理角色，创建代理信息并生成随机邀请码
        if ("agent".equals(user.getRole())) {
            Agent agent = new Agent();
            agent.setUserId(user.getId());
            agent.setInviteCode(generateUniqueInviteCode());
            agentMapper.insert(agent);
        }
        return Result.success();
    }

    // ==================== 数据概览 ====================

    @GetMapping("/dashboard")
    public Result<?> getDashboard() {
        return Result.success(adminService.getDashboard());
    }

    // ==================== 玩家管理 ====================

    @GetMapping("/players")
    public Result<?> getPlayers(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int size,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) Long agentId,
                                @RequestParam(required = false) String role) {
        return Result.success(adminService.getPlayers(page, size, keyword, agentId, role));
    }

    @PostMapping("/players/{id}/tokens")
    public Result<?> updateTokens(@PathVariable Long id, @RequestBody TokenDTO dto) {
        Long operatorId = AuthInterceptor.CURRENT_USER_ID.get();
        adminService.updateTokens(id, dto.getAmount(), dto.getRemark(), operatorId);
        return Result.success();
    }

    @PostMapping("/players/{id}/status")
    public Result<?> setStatus(@PathVariable Long id, @RequestBody StatusDTO dto) {
        adminService.setStatus(id, dto.getStatus());
        return Result.success();
    }

    @GetMapping("/players/{userId}/warehouse")
    public Result<?> getWarehouseItems(@PathVariable Long userId) {
        return Result.success(adminService.getWarehouseItems(userId));
    }

    @PostMapping("/players/{userId}/warehouse")
    public Result<?> addWarehouseItem(@PathVariable Long userId, @RequestBody WarehouseDTO dto) {
        adminService.addWarehouseItem(userId, dto.getPrizeId());
        return Result.success();
    }

    @DeleteMapping("/players/{userId}/warehouse/{itemId}")
    public Result<?> removeWarehouseItem(@PathVariable Long userId, @PathVariable Long itemId) {
        adminService.removeWarehouseItem(userId, itemId);
        return Result.success();
    }

    // ==================== 奖品管理 ====================

    @GetMapping("/prizes")
    public Result<?> getPrizes(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return Result.success(adminService.getPrizes(page, size));
    }

    @PostMapping("/prizes")
    public Result<?> createPrize(@RequestBody Prize prize) {
        adminService.createPrize(prize);
        return Result.success();
    }

    @PutMapping("/prizes/{id}")
    public Result<?> updatePrize(@PathVariable Long id, @RequestBody Prize prize) {
        adminService.updatePrize(id, prize);
        return Result.success();
    }

    @DeleteMapping("/prizes/{id}")
    public Result<?> deletePrize(@PathVariable Long id) {
        adminService.deletePrize(id);
        return Result.success();
    }

    // ==================== 抽奖配置 ====================

    @GetMapping("/lottery/config")
    public Result<?> getLotteryConfig() {
        return Result.success(adminService.getLotteryConfig());
    }

    @PutMapping("/lottery/config")
    public Result<?> updateLotteryConfig(@RequestBody LotteryConfig config) {
        adminService.updateLotteryConfig(config);
        return Result.success();
    }

    // ==================== 充值管理 ====================

    @GetMapping("/recharge")
    public Result<?> getRechargeOrders(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) String keyword) {
        return Result.success(adminService.getRechargeOrders(page, size, status, keyword));
    }

    @PostMapping("/recharge/{id}/confirm")
    public Result<?> confirmRecharge(@PathVariable Long id) {
        Long operatorId = AuthInterceptor.CURRENT_USER_ID.get();
        adminService.confirmRecharge(id, operatorId);
        return Result.success();
    }

    @PostMapping("/recharge/{id}/reject")
    public Result<?> rejectRecharge(@PathVariable Long id, @RequestBody RejectDTO dto) {
        adminService.rejectRecharge(id, dto.getReason());
        return Result.success();
    }

    // ==================== 发货管理 ====================

    @GetMapping("/orders")
    public Result<?> getOrders(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) String status) {
        return Result.success(adminService.getDeliveryOrders(page, size, status));
    }

    @PostMapping("/orders/{id}/ship")
    public Result<?> shipOrder(@PathVariable Long id, @RequestBody(required = false) ShipDTO dto) {
        adminService.shipOrder(id,
                dto != null ? dto.getExpressCompany() : null,
                dto != null ? dto.getExpressNo() : null);
        return Result.success();
    }

    @PostMapping("/orders/{id}/done")
    public Result<?> completeOrder(@PathVariable Long id) {
        adminService.completeOrder(id);
        return Result.success();
    }

    // ==================== 代理管理 ====================

    @GetMapping("/agents")
    public Result<?> getAgents(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return Result.success(adminService.getAgents(page, size));
    }

    // ==================== 市场管理 ====================

    @GetMapping("/market")
    public Result<?> getMarket(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) String status) {
        return Result.success(adminService.getMarket(page, size, status));
    }

    @DeleteMapping("/market/{id}")
    public Result<?> removeMarketItem(@PathVariable Long id) {
        adminService.removeMarketItem(id);
        return Result.success();
    }

    // ==================== 售后管理 ====================

    @GetMapping("/after-sale")
    public Result<?> getAfterSales(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(required = false) String status) {
        return Result.success(adminService.getAfterSales(status, page, size));
    }

    @PostMapping("/after-sale/{id}/approve")
    public Result<?> approveAfterSale(@PathVariable Long id) {
        Long operatorId = AuthInterceptor.CURRENT_USER_ID.get();
        adminService.approveAfterSale(id, operatorId);
        return Result.success();
    }

    @PostMapping("/after-sale/{id}/reject")
    public Result<?> rejectAfterSale(@PathVariable Long id, @RequestBody RejectDTO dto) {
        Long operatorId = AuthInterceptor.CURRENT_USER_ID.get();
        adminService.rejectAfterSale(id, operatorId, dto.getReason());
        return Result.success();
    }

    @PostMapping("/after-sale/{id}/done")
    public Result<?> completeAfterSale(@PathVariable Long id) {
        Long operatorId = AuthInterceptor.CURRENT_USER_ID.get();
        adminService.completeAfterSale(id, operatorId);
        return Result.success();
    }

    // ==================== 支付配置 ====================

    @GetMapping("/config/payment")
    public Result<?> getPaymentConfig() {
        return Result.success(adminService.getPaymentConfig());
    }

    @PostMapping("/config/payment")
    public Result<?> savePaymentConfig(@RequestBody PaymentConfigDTO dto) {
        adminService.savePaymentConfig(dto.getTaobaoLink(), dto.getPaymentNotice());
        return Result.success();
    }

    // ==================== 导出 ====================

    @GetMapping("/recharge/export")
    public ResponseEntity<byte[]> exportRechargeOrders(@RequestParam(required = false) String status) {
        byte[] data = adminService.exportRechargeOrders(status);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recharge_orders.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/orders/export")
    public ResponseEntity<byte[]> exportDeliveryOrders(@RequestParam(required = false) String status) {
        byte[] data = adminService.exportDeliveryOrders(status);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=delivery_orders.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    // ==================== DTO ====================

    @Data
    static class CreateUserDTO {
        private String username;
        private String password;
        private String nickname;
        private String role; // player / agent / admin
    }

    @Data
    static class TokenDTO {
        private int amount;
        private String remark;
    }

    @Data
    static class StatusDTO {
        private int status;
    }

    @Data
    static class WarehouseDTO {
        private Long prizeId;
    }

    @Data
    static class RejectDTO {
        private String reason;
    }

    @Data
    static class ShipDTO {
        private String expressCompany;
        private String expressNo;
    }

    @Data
    static class PaymentConfigDTO {
        private String taobaoLink;
        private String paymentNotice;
    }

    private String generateUniqueInviteCode() {
        for (int i = 0; i < 10; i++) {
            String code = RandomUtil.randomAlphanumeric(6).toUpperCase();
            if (agentMapper.findByInviteCode(code) == null) {
                return code;
            }
        }
        throw new com.lottery.common.exception.BusinessException("邀请码生成失败，请重试");
    }
}
