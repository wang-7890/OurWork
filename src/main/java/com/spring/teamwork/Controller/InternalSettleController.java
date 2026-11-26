package com.spring.teamwork.Controller;

import com.spring.teamwork.Service.SettlementService;
import com.spring.teamwork.model.DTO.SettleTriggerRequest;
import com.spring.teamwork.model.Enum.ContractStatus;
import com.spring.teamwork.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部结算接口（供 Verdict 模块远程触发结算，调用方为 SettleRemoteClient）。
 * 语义是"受理"而非"结算成功"：已结算/正在结算/合约不存在的情况均返回 code=0，
 * 由超时裁决定时任务兜底；仅双保险同时失效（影响行数为0）才经全局处理器返回 409
 */
@RestController
@RequestMapping("/api/internal")
public class InternalSettleController {

    @Autowired
    private SettlementService settlementService;

    @PostMapping("/settle/{contractId}")
    public ResponseEntity<Result<Void>> triggerSettle(@PathVariable Long contractId,
                                                      @RequestBody @Validated SettleTriggerRequest request) {
        // 调用方传 Verdict 取值（SUCCESS/FAIL），映射为合约终态（SUCCESS/FAILED）
        ContractStatus status = "SUCCESS".equals(request.getResult())
                ? ContractStatus.SUCCESS
                : ContractStatus.FAILED;
        settlementService.settle(contractId, status);
        return ResponseEntity.ok(Result.ok());
    }
}
