package com.spring.teamwork.Service;

public interface DemoService {
    void expireContract(Long contractId,Long operatorUserId);
    void resetAll();
    void triggerSettleJobs();
}
