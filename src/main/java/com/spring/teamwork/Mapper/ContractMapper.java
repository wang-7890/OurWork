package com.spring.teamwork.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spring.teamwork.model.Entity.BetContract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ContractMapper extends BaseMapper<BetContract> {
    @Select("SELECT COUNT(*) FROM bet_contract WHERE id = #{contractId}")
    int countById(@Param("contractId") Long contractId);
    @Select("SELECT title FROM bet_contract WHERE id = #{contractId}")
    String getTitle(@Param("contractId") Long contractId);
}
