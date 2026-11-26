package com.spring.teamwork.Controller;

import cn.dev33.satoken.stp.StpUtil;
import com.spring.teamwork.Service.SquareService;
import com.spring.teamwork.model.DTO.SquareQueryRequest;
import com.spring.teamwork.model.PageVO;
import com.spring.teamwork.model.Result;
import com.spring.teamwork.model.VO.SquareContractItemVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/api/square")
@RequiredArgsConstructor
public class SquareController {
    private final SquareService squareService;

    @GetMapping("/contracts")
    public Result<PageVO<SquareContractItemVO>> list(@Valid SquareQueryRequest req) {
        return Result.ok(squareService.pagePublicContracts(req));
    }
    @PostMapping("/contracts/{contractId}/view")
    public Result<Void> view(@PathVariable Long contractId, HttpServletRequest request) {
        Long userId = null;
        try {
            userId = StpUtil.getLoginIdAsLong();
        } catch (Exception ignored) {
        }

        String anonymousKey = null;
        if (userId == null) {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = request.getRemoteAddr();
            }
            String ua = request.getHeader("User-Agent");
            anonymousKey = sha256Hex((ip + "|" + (ua != null ? ua : "")).getBytes());
        }

        squareService.incrementViewCount(contractId, userId, anonymousKey);
        return Result.ok();
    }
    private static String sha256Hex(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 必带算法，不可能走到这里；兜底返回固定字符串
            return "anon";
        }
    }



}
