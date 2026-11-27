package com.spring.teamwork.Controller;

import cn.dev33.satoken.stp.StpUtil;
import com.spring.teamwork.Service.DemoService;
import com.spring.teamwork.model.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {
    private final DemoService demoService;
    @PostMapping("/contracts/{contractId}/expire")
    public Result<Void> expire(@PathVariable Long contractId) {
        Long userId = null;
        try { userId = StpUtil.getLoginIdAsLong(); } catch (Exception ignored) {}
        demoService.expireContract(contractId, userId);
        return Result.ok();
    }
    @PostMapping("/jobs/settle")
    public Result<Void> triggerSettle() {
        demoService.triggerSettleJobs();
        return Result.ok();
    }
    @Value("${demo.enabled:false}")
    private boolean demoEnabled;
    @PostMapping("/reset")
    public Result<Void> reset() {
        if (!demoEnabled) {
            return Result.fail(403, "演示模式未开启，无法执行重置操作");
        }
        demoService.resetAll();
        return Result.ok();
    }


}
