package com.lottery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lottery.entity.Market;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MarketMapper extends BaseMapper<Market> {

    /**
     * 原子抢购：只有 status='on' 时才更新为 'sold'，防止并发双卖
     */
    @Update("UPDATE market SET status = 'sold', buyer_id = #{buyerId}, sold_at = NOW() WHERE id = #{id} AND status = 'on'")
    int atomicBuy(@Param("id") Long id, @Param("buyerId") Long buyerId);
}
