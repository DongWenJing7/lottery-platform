package com.lottery.controller;

import com.lottery.common.Result;
import com.lottery.entity.Agent;
import com.lottery.entity.User;
import com.lottery.mapper.AgentMapper;
import com.lottery.mapper.UserMapper;
import com.lottery.util.JwtUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final AgentMapper agentMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginDTO dto) {
        User user = userMapper.findByUsername(dto.getUsername());
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            return Result.error(400, "手机号/用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            return Result.error(400, "账号已被封禁");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("role", user.getRole());
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("balance", user.getBalance());
        userInfo.put("username", user.getUsername());
        data.put("userInfo", userInfo);
        return Result.success(data);
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterDTO dto) {
        if (userMapper.findByUsername(dto.getUsername()) != null) {
            return Result.error(400, "该手机号已注册");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        String phone = dto.getUsername();
        String defaultNick = "用户" + phone.substring(Math.max(0, phone.length() - 4));
        user.setNickname(dto.getNickname() != null && !dto.getNickname().isEmpty() ? dto.getNickname() : defaultNick);
        user.setRole("player");
        user.setBalance(0);
        user.setStatus(1);

        // 邀请码绑定代理：通过随机邀请码查找代理
        if (StringUtils.hasText(dto.getInviteCode())) {
            Agent inviteAgent = agentMapper.findByInviteCode(dto.getInviteCode().toUpperCase());
            if (inviteAgent != null) {
                user.setAgentId(inviteAgent.getUserId());
            } else {
                return Result.error(400, "邀请码无效");
            }
        }

        userMapper.insert(user);

        // 如果绑定了代理，更新代理的团队人数并实时判定等级
        if (user.getAgentId() != null) {
            Agent agent = agentMapper.findByUserId(user.getAgentId());
            if (agent != null) {
                int teamCount = (agent.getTeamCount() != null ? agent.getTeamCount() : 0) + 1;
                agent.setTeamCount(teamCount);
                int teamRecharge = agent.getTeamRecharge() != null ? agent.getTeamRecharge() : 0;
                recalcAgentLevel(agent, teamCount, teamRecharge);
                agentMapper.updateById(agent);
            }
        }
        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("role", user.getRole());
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("balance", 0);
        data.put("userInfo", userInfo);
        return Result.success(data);
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        return Result.success();
    }

    @Data
    static class LoginDTO {
        private String username;
        private String password;
    }

    @Data
    static class RegisterDTO {
        private String username;
        private String password;
        private String nickname;
        private String inviteCode;
    }

    private void recalcAgentLevel(Agent agent, int teamCount, int teamRecharge) {
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
    }
}
