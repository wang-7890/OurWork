package com.spring.teamwork.model.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 结算触发请求（内部接口，Verdict 模块通过 SettleRemoteClient 调用）。
 * 只接收 result 字段；trigger/caller 等额外字段由 Jackson 自动忽略
 */
@Data
public class SettleTriggerRequest {

    /** Verdict 取值：SUCCESS / FAIL（注意是 FAIL 不是 FAILED） */
    @NotBlank
    private String result;
}
