package com.spring.teamwork.Controller;

import cn.dev33.satoken.stp.StpUtil;
import com.spring.teamwork.Service.CreditService;
import com.spring.teamwork.model.Result;
import com.spring.teamwork.model.VO.CreditVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 诚信档案（接口文档 第 6 节）。
 * 6.1 GET  /api/profile/credit       → 登录（从 token 取 userId）
 * 6.2 GET  /api/profile/credit/{id}  → SaTokenConfig 放行（公开可查，自动隐藏敏感字段）
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;

    @GetMapping("/credit")
    public Result<CreditVO.Profile> myCredit() {
        return Result.ok(creditService.getMyProfile(StpUtil.getLoginIdAsLong()));
    }
    @GetMapping("/credit/{userId}")
    public Result<CreditVO.Profile> otherCredit(@PathVariable Long userId) {
        return Result.ok(creditService.getOtherProfile(userId));
    }
}
