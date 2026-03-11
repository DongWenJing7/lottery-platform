package com.lottery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lottery.entity.Prize;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PrizeMapper extends BaseMapper<Prize> {

    /**
     * 原子扣减库存，stock > 0 时才扣，防止超卖。
     * stock = -1 表示无限库存，不走此方法。
     * 返回影响行数：1=成功，0=库存不足。
     */
    @Update("UPDATE prize SET stock = stock - 1, status = CASE WHEN stock - 1 = 0 THEN 0 ELSE status END WHERE id = #{prizeId} AND stock > 0")
    int deductStock(@Param("prizeId") Long prizeId);
}
