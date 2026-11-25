package com.spring.teamwork.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spring.teamwork.model.Entity.BetVote;
import com.spring.teamwork.model.Enum.Verdict;
import com.spring.teamwork.model.VO.ContractDetailVO;
import com.spring.teamwork.model.VO.VerdictVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BetVoteMapper extends BaseMapper<BetVote> {
    /** 合约总票数 */
    @Select("SELECT COUNT(*) FROM bet_vote WHERE contract_id = #{contractId}")
    int countByContractId(@Param("contractId") Long contractId);

    /** 按选项统计 */
    @Select("SELECT COUNT(*) FROM bet_vote WHERE contract_id = #{contractId} AND verdict = #{verdict}")
    int countByContractIdAndVerdict(@Param("contractId") Long contractId,
                                    @Param("verdict") Verdict verdict);

    /** 查全部投票明细（含关联用户昵称） */
    @Select("SELECT v.user_id AS userId, " +
            "       u.nickname AS nickname, " +
            "       v.verdict AS verdict, " +
            "       v.comment AS comment, " +
            "       v.created_at AS votedAt " +
            "FROM bet_vote v " +
            "LEFT JOIN sys_user u ON u.id = v.user_id " +
            "WHERE v.contract_id = #{contractId} " +
            "ORDER BY v.created_at ASC")
    List<VerdictVO.VoteResult.VoteDetail> selectVoteDetails(@Param("contractId") Long contractId);

    /** 查某个用户在某合约是否已投票（防重复） */
    @Select("SELECT COUNT(*) FROM bet_vote WHERE contract_id = #{contractId} AND user_id = #{userId}")
    int countByContractIdAndUserId(@Param("contractId") Long contractId,
                                   @Param("userId") Long userId);

}
