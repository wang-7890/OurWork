package com.spring.teamwork.Service;

import com.spring.teamwork.model.Enum.TransactionType;
import com.spring.teamwork.model.PageVO;
import com.spring.teamwork.model.Result;
import com.spring.teamwork.model.VO.BalanceVO;
import com.spring.teamwork.model.VO.TransactionVO;

public interface AccountService {
    Result<BalanceVO> myAccount(Long userId);

    Result<PageVO<TransactionVO>> getList(Integer page, Integer size, TransactionType type, Long userId);
}
