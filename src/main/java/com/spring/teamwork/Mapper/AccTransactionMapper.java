package com.spring.teamwork.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spring.teamwork.model.Entity.AccTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AccTransactionMapper extends BaseMapper<AccTransaction> {
    @Select("SELECT COALESCE(SUM(amount), 0) FROM acc_transaction WHERE user_id = #{userId} AND remark = 'DEDUCT'")
    int countDeducted(@Param("userId") Long userId);
}
