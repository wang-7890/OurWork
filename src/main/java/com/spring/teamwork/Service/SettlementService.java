package com.spring.teamwork.Service;

import com.spring.teamwork.model.Enum.ContractStatus;

/**
 * 定时任务服务
 */
public interface SettlementService {

    void settle(Long contractId, ContractStatus result);
    /**
     * 到期扫描，ACTIVE 且 deadline < now → VOTING，给见证人发裁决通知，30s执行一次
     */
    void expireScan();

    /**
     * 超时裁决，VOTING 超过 24 小时 → FAILED 结算，需要调用settle,10分钟执行一次
     */
    void timeoutSettle();

    /**
     * 围观数落库，Redis viewCount 增量写入 MySQL
     */
    void viewCountFlush();
}
