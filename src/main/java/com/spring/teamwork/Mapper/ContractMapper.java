package com.spring.teamwork.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spring.teamwork.model.Entity.BetContract;
import com.spring.teamwork.model.Enum.ContractStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ContractMapper extends BaseMapper<BetContract> {
    @Select("SELECT COUNT(*) FROM bet_contract WHERE id = #{contractId};")
    int countById(@Param("contractId") Long contractId);
    @Select("SELECT title FROM bet_contract WHERE id = #{contractId};")
    String getTitle(@Param("contractId") Long contractId);
    @Select("SELECT * FROM bet_contract WHERE id = #{contractId} FOR UPDATE;")
    BetContract selectByIdForUpdate(@Param("contractId") Long contractId);
    @Update("UPDATE bet_contract SET status = #{result},settle_result = #{result},settle_at = NOW()" +
            "WHERE id = #{contractId};")
    int updateStatusWhenVoting(@Param("contractId") Long contractId, @Param("result")String result);
    @Update("UPDATE bet_contract SET status = 'VOTING',updated_at = NOW() WHERE id = #{contractId}" +
            "AND status = 'ACTIVE' AND is_deleted = 0;")
    int activeTOVoting(@Param("contractId") Long contractId);

}
