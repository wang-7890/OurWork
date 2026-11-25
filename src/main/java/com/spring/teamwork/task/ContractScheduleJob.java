package com.spring.teamwork.task;

import com.spring.teamwork.Service.SettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ContractScheduleJob {
    @Autowired
    private SettlementService settlementService;

    @Scheduled(fixedDelay = 30_000)
    public void expireScan(){
        settlementService.expireScan();
    }

    @Scheduled(fixedDelay = 300_000)
    public void timeoutSettle(){
        settlementService.timeoutSettle();
    }
}
