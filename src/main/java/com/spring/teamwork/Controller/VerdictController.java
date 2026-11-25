package com.spring.teamwork.Controller;

import cn.dev33.satoken.stp.StpUtil;
import com.spring.teamwork.Service.VerdictService;
import com.spring.teamwork.model.DTO.VoteRequest;
import com.spring.teamwork.model.PageVO;
import com.spring.teamwork.model.Result;
import com.spring.teamwork.model.VO.VerdictVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VerdictController {
    private final VerdictService verdictService;

    @GetMapping("/verdicts/pending")
    public Result<PageVO<VerdictVO.PendingItem>> pending(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        size = Math.min(size, 50);
        return Result.ok(verdictService.pagePending(StpUtil.getLoginIdAsLong(), page, size));
    }
    @PostMapping("/contracts/{contractId}/vote")
    public Result<VerdictVO.VoteResponse> vote(@PathVariable Long contractId,
                                       @Valid @RequestBody VoteRequest req) {
        return Result.ok(verdictService.submitVote(contractId, req.getVerdict(),
                req.getComment(), StpUtil.getLoginIdAsLong()));
    }
    @GetMapping("/contracts/{contractId}/votes")
    public Result<VerdictVO.VoteResult> voteResult(@PathVariable Long contractId) {
        return Result.ok(verdictService.getVoteResult(contractId, StpUtil.getLoginIdAsLong()));
    }
}

