package com.spring.teamwork.Service;

import com.spring.teamwork.model.Enum.Verdict;
import com.spring.teamwork.model.PageVO;
import com.spring.teamwork.model.VO.VerdictVO;


public interface VerdictService {
    /*裁决列表*/
    PageVO<VerdictVO.PendingItem> pagePending(Long userId, Integer page, Integer size);
    /*提交投票*/
    VerdictVO.VoteResponse submitVote(Long contractId, Verdict verdict, String comment,Long userId);
    /*查看结果*/
    VerdictVO.VoteResult getVoteResult(Long contractId, Long currentUserId);

}
