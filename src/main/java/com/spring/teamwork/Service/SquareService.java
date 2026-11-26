package com.spring.teamwork.Service;

import com.spring.teamwork.model.DTO.SquareQueryRequest;
import com.spring.teamwork.model.PageVO;
import com.spring.teamwork.model.VO.SquareContractItemVO;

public interface SquareService {
     PageVO<SquareContractItemVO> pagePublicContracts(SquareQueryRequest req);

    void incrementViewCount(Long contractId,Long userId,String anonymousKey);
    int getViewCount(Long contractId);
}
