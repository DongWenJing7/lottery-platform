package com.lottery.controller;

import com.lottery.common.Result;
import com.lottery.interceptor.AuthInterceptor;
import com.lottery.service.PlayerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/player")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping("/info")
    public Result<?> getInfo() {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        return Result.success(playerService.getInfo(userId));
    }

    @PutMapping("/bindPhone")
    public Result<?> bindPhone(@RequestBody BindPhoneDTO dto) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        playerService.bindPhone(userId, dto.getPassword(), dto.getPhone());
        return Result.success();
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody ProfileDTO dto) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        playerService.updateProfile(userId, dto.getNickname(), dto.getAvatar(), dto.getAddress());
        return Result.success();
    }

    @GetMapping("/logs")
    public Result<?> getLogs(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "20") int size,
                             @RequestParam(required = false) String type) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        return Result.success(playerService.getLogs(userId, page, size, type));
    }

    @GetMapping("/recharge")
    public Result<?> getRechargeOrders(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        return Result.success(playerService.getRechargeOrders(userId, page, size));
    }

    @PostMapping("/recharge")
    public Result<?> submitRecharge(@RequestBody RechargeDTO dto) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        return Result.success(playerService.submitRecharge(userId, dto.getTokens(), dto.getAmount(), dto.getRemark()));
    }

    @GetMapping("/warehouse")
    public Result<?> getWarehouse(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(required = false) String status) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        return Result.success(playerService.getWarehouse(userId, page, size, status));
    }

    @DeleteMapping("/warehouse/{id}")
    public Result<?> deleteItem(@PathVariable Long id) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        playerService.deleteItem(userId, id);
        return Result.success();
    }

    @PostMapping("/warehouse/{id}/sell")
    public Result<?> listForSale(@PathVariable Long id, @RequestBody SellDTO dto) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        playerService.listForSale(userId, id, dto.getPrice());
        return Result.success();
    }

    @PostMapping("/warehouse/{id}/unsell")
    public Result<?> unsell(@PathVariable Long id) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        playerService.unsell(userId, id);
        return Result.success();
    }

    @PostMapping("/warehouse/{id}/deliver")
    public Result<?> requestDelivery(@PathVariable Long id, @RequestBody DeliveryDTO dto) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        return Result.success(playerService.requestDelivery(userId, id,
                dto.getReceiverName(), dto.getReceiverPhone(), dto.getReceiverAddress()));
    }

    @GetMapping("/orders")
    public Result<?> getOrders(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) String status) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        return Result.success(playerService.getOrders(userId, page, size, status));
    }

    @Data
    static class RechargeDTO {
        private int tokens;
        private double amount;
        private String remark;
    }

    @Data
    static class SellDTO {
        private int price;
    }

    @Data
    static class DeliveryDTO {
        private String receiverName;
        private String receiverPhone;
        private String receiverAddress;
    }

    @Data
    static class BindPhoneDTO {
        private String password;
        private String phone;
    }

    @Data
    static class ProfileDTO {
        private String nickname;
        private String avatar;
        private String address;
    }
}
