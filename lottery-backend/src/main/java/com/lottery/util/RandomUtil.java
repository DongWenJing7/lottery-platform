package com.lottery.util;

import com.lottery.entity.Prize;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RandomUtil {

    private static final String ALPHANUMERIC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public static String randomAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(ThreadLocalRandom.current().nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * 加权随机抽取一个奖品（基于概率百分比，如 0.9 表示 0.9%）
     */
    public Prize weightedRandom(List<Prize> prizes) {
        if (prizes == null || prizes.isEmpty()) {
            return null;
        }
        double totalWeight = prizes.stream().mapToDouble(Prize::getProbability).sum();
        if (totalWeight <= 0) {
            return prizes.get(ThreadLocalRandom.current().nextInt(prizes.size()));
        }
        double rand = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cumulative = 0;
        for (Prize prize : prizes) {
            cumulative += prize.getProbability();
            if (rand < cumulative) {
                return prize;
            }
        }
        return prizes.get(prizes.size() - 1);
    }
}
