package com.spring.teamwork.Controller;

import cn.dev33.satoken.stp.StpUtil;
import com.spring.teamwork.Service.AccountService;
import com.spring.teamwork.model.Enum.ContractStatus;
import com.spring.teamwork.model.Enum.TransactionType;
import com.spring.teamwork.model.PageVO;
import com.spring.teamwork.model.Result;
import com.spring.teamwork.model.VO.BalanceVO;
import com.spring.teamwork.model.VO.TransactionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AccountController {
    @Autowired
    private AccountService accountService;

    @GetMapping("/account/balance")
    public ResponseEntity<Result<BalanceVO>> getMyAccount(){
        Long userId = StpUtil.getLoginIdAsLong();
        return ResponseEntity.ok(accountService.myAccount(userId));
    }

    @GetMapping("/account/transactions")
    public ResponseEntity<Result<PageVO<TransactionVO>>> getAccountList(@RequestParam(required = false) TransactionType type,
                                                                        @RequestParam(defaultValue = "1") Integer page,
                                                                        @RequestParam(defaultValue = "10") Integer size){
        Long userId = StpUtil.getLoginIdAsLong();
        return ResponseEntity.ok(accountService.getList(page,size,type,userId));
    }
}
