package com.spring.teamwork.Controller;
import com.spring.teamwork.Service.UserService;

import com.spring.teamwork.model.DTO.AuthRequest;
import com.spring.teamwork.model.Result;
import com.spring.teamwork.model.VO.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证与用户模块（接口文档 第 2 节）。
 *
 * 放行接口（SaTokenConfig 已配置）：/login /register
 * 需登录接口：/logout /profile
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
   @Autowired
    private final UserService userService;

    /** 注册（2.1）—— 注册即登录，返回 token + giftAmount */
    @PostMapping("/register")
    public Result<UserVO.RegisterVO> register(@Valid @RequestBody AuthRequest.RegisterRequest req) {
        return Result.ok(userService.register(req));
    }

    /** 登录（2.2） */
    @PostMapping("/login")
    public Result<UserVO.LoginVO> login(@Valid @RequestBody AuthRequest.LoginRequest req) {
        return Result.ok(userService.login(req));
    }

    /** 登出（2.3） */
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.ok();
    }

    /** 当前用户信息（2.4） */
    @GetMapping("/profile")
    public Result<UserVO.UserProfileVO> profile() {
        return Result.ok(userService.getCurrentUserProfile());
    }
}
