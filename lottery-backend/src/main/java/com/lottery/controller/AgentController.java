package com.lottery.controller;

import com.lottery.common.Result;
import com.lottery.interceptor.AuthInterceptor;
import com.lottery.service.AgentService;
import com.lottery.service.PlayerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final PlayerService playerService;

    @GetMapping("/info")
    public Result<?> getInfo() {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        return Result.success(playerService.getInfo(userId));
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody ProfileDTO dto) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        playerService.updateProfile(userId, dto.getNickname(), dto.getAvatar(), dto.getAddress());
        return Result.success();
    }

    @GetMapping("/dashboard")
    public Result<?> getDashboard() {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        return Result.success(agentService.getDashboard(userId));
    }

    @GetMapping("/players")
    public Result<?> getPlayers(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int size,
                                @RequestParam(required = false) String keyword) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        return Result.success(agentService.getPlayers(userId, page, size, keyword));
    }

    @GetMapping("/commission")
    public Result<?> getCommission(@RequestParam(required = false) Integer year,
                                   @RequestParam(required = false) Integer month) {
        Long userId = AuthInterceptor.CURRENT_USER_ID.get();
        if (year == null) year = LocalDate.now().getYear();
        if (month == null) month = LocalDate.now().getMonthValue();
        return Result.success(agentService.getCommission(userId, year, month));
    }

    @Data
    static class ProfileDTO {
        private String nickname;
        private String avatar;
        private String address;
    }
}
