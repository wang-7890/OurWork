package com.spring.teamwork.Controller;

import cn.dev33.satoken.stp.StpUtil;
import com.spring.teamwork.Service.ContractService;
import com.spring.teamwork.model.DTO.ContractCreateRequest;
import com.spring.teamwork.model.DTO.ProofRequest;
import com.spring.teamwork.model.Enum.ContractStatus;
import com.spring.teamwork.model.PageVO;
import com.spring.teamwork.model.Result;
import com.spring.teamwork.model.VO.AcceptInviteVO;
import com.spring.teamwork.model.VO.ContractDetailVO;
import com.spring.teamwork.model.VO.ContractListVO;
import com.spring.teamwork.model.VO.WitnessInviteVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 合约模块
 */
@RestController
@RequestMapping("/api")
public class ContractController {

    @Autowired
    private ContractService contractService;

    @PostMapping("/contracts")
    public ResponseEntity<Result<ContractDetailVO>> createContract(@RequestBody @Validated ContractCreateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Result<ContractDetailVO> result = contractService.create(request, userId);
        if (result.getCode() == 0) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(result.getCode()).body(result);
    }

    @GetMapping("/contracts/my")
    public ResponseEntity<Result<PageVO<ContractListVO>>> myContract(
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        size = Math.min(size, 50);
        return ResponseEntity.ok(contractService.getMyContract(status, page, size, userId));
    }

    @GetMapping("/contracts/{contractId}")
    public ResponseEntity<Result<ContractDetailVO>> contractDetail(@PathVariable Integer contractId) {
        return ResponseEntity.ok(contractService.getContractDetail(contractId));
    }

    @PostMapping("/contracts/{contractId}/witness-invite")
    public ResponseEntity<Result<WitnessInviteVO>> createInvite(@PathVariable Integer contractId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResponseEntity.ok(contractService.createInviteUrl(contractId, userId));
    }

    @PostMapping("/witness-invites/{inviteCode}/accept")
    public ResponseEntity<Result<AcceptInviteVO>> acceptInvite(@PathVariable String inviteCode) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResponseEntity.ok(contractService.accept(inviteCode, userId));
    }

    @PostMapping("/contracts/{contractId}/proof")
    public ResponseEntity<Result<ContractDetailVO.ProofVO>> submit(@PathVariable Long contractId, @RequestBody @Validated ProofRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResponseEntity.ok(contractService.submitProof(contractId, request, userId));
    }
}
