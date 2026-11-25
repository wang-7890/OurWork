package com.spring.teamwork.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spring.teamwork.Mapper.AccTransactionMapper;
import com.spring.teamwork.Mapper.ContractMapper;
import com.spring.teamwork.Mapper.SysUserMapper;
import com.spring.teamwork.Service.AccountService;
import com.spring.teamwork.model.Entity.AccTransaction;
import com.spring.teamwork.model.Entity.SysUser;
import com.spring.teamwork.model.Enum.TransactionType;
import com.spring.teamwork.model.PageVO;
import com.spring.teamwork.model.Result;
import com.spring.teamwork.model.VO.BalanceVO;
import com.spring.teamwork.model.VO.TransactionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private AccTransactionMapper accTransactionMapper;
    @Autowired
    private ContractMapper contractMapper;

    @Override
    public Result<BalanceVO> myAccount(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        BalanceVO balanceVO = new BalanceVO();
        balanceVO.setBalance(user.getBalance());
        balanceVO.setFrozen(user.getFrozen());
        balanceVO.setTotalDeducted(accTransactionMapper.countDeducted(userId));
        return Result.ok(balanceVO);
    }

    @Override
    public Result<PageVO<TransactionVO>> getList(Integer page, Integer size, TransactionType type, Long userId) {
        LambdaQueryWrapper<AccTransaction> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(AccTransaction::getUserId,userId);
        if(type!=null){
            queryWrapper.eq(AccTransaction::getType,type);
        }
        Page<AccTransaction> accTransactionPage = accTransactionMapper.selectPage(new Page<>(page,size),queryWrapper);
        List<TransactionVO> transactionVOS = accTransactionPage.getRecords().stream()
                .map(accTransaction -> {
                    TransactionVO transactionVO = new TransactionVO();
                    transactionVO.setTransactionId(accTransaction.getId());
                    transactionVO.setCreatedAt(accTransaction.getCreatedAt());
                    transactionVO.setType(accTransaction.getType());
                    transactionVO.setContractId(accTransaction.getContractId());
                    transactionVO.setAmount(accTransaction.getAmount());
                    transactionVO.setBalanceAfter(accTransaction.getBalanceAfter());
                    transactionVO.setRemark(accTransaction.getRemark());
                    transactionVO.setContractTitle(contractMapper.getTitle(accTransaction.getContractId()));
                    return transactionVO;
                }).toList();
        return Result.ok(new PageVO<>(accTransactionPage.getTotal(),accTransactionPage.getCurrent(),accTransactionPage.getSize(),transactionVOS));
    }
}
