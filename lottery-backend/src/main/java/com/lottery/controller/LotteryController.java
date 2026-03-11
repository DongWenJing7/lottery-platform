package com.lottery.controller;

import com.lottery.common.Result;
import com.lottery.interceptor.AuthInterceptor;
import com.lottery.service.LotteryService;
import com.lottery.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LotteryController {

    private final LotteryService lotteryService;
    private final PlayerService playerService;

    @GetMapping("/api/lottery/config")
    public Result<?> getConfig() {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        return Result.success(lotteryService.getConfig(userId));
    }

    @PostMapping("/api/lottery/draw")
    public Result<?> draw() {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        return Result.success(lotteryService.draw(userId));
    }

    @GetMapping("/api/market/list")
    public Result<?> getMarket(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) String keyword) {
        return Result.success(playerService.getMarket(page, size, keyword));
    }

    @PostMapping("/api/market/{id}/buy")
    public Result<?> buyItem(@PathVariable Long id) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        return Result.success(playerService.buyItem(userId, id));
    }
}
