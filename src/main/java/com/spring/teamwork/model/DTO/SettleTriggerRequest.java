package com.spring.teamwork.model.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SettleTriggerRequest {
    @NotBlank(message = "触发方式不能为空")
    private String trigger;    // vote / demo / schedule
    private String caller;     // verdict / demo / schedule
    /** SUCCESS / FAIL —— Verdict 传来的裁决结果 */
    private String result;

}
