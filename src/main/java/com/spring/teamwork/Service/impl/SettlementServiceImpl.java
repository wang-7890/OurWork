package com.spring.teamwork.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring.teamwork.Exception.common.BizException;
import com.spring.teamwork.Mapper.*;
import com.spring.teamwork.Service.NoticeService;
import com.spring.teamwork.Service.SettlementService;
import com.spring.teamwork.model.Entity.*;
import com.spring.teamwork.model.Enum.ContractStatus;
import com.spring.teamwork.model.Enum.NotificationType;
import com.spring.teamwork.model.Enum.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.spring.teamwork.Service.NoticeService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class SettlementServiceImpl implements SettlementService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private ContractMapper contractMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private BetContractWitnessMapper betContractWitnessMapper;
    @Autowired
    private SysNotificationMapper sysNotificationMapper;
    @Autowired
    private AccTransactionMapper accTransactionMapper;
    @Autowired
    private NoticeService noticeService;



    @Override
    public void settle(Long contractId, ContractStatus result) {
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent("contract:settle:lock:"+contractId,"1", Duration.ofSeconds(10));
        if (!Boolean.TRUE.equals(ok)){
            System.out.println("合约 " + contractId + " 正被其他路径结算，跳过");
            return;
        }
        try {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.executeWithoutResult(status -> doSettle(contractId,result));
        } finally {
            stringRedisTemplate.delete("contract:settle:lock:"+contractId);
        }
    }

    private void doSettle(Long contractId, ContractStatus result) {
        BetContract betContract = contractMapper.selectByIdForUpdate(contractId);
        if (betContract == null || betContract.getStatus() != ContractStatus.VOTING){
            return;
        }
        Long userId = betContract.getUserId();
        Integer deposit = betContract.getDeposit();
        if(result == ContractStatus.SUCCESS){
            sysUserMapper.unfreezeDeposit(userId,deposit);
        }else {
            sysUserMapper.deductDeposit(userId,deposit);
        }

        SysUser user = sysUserMapper.selectById(userId);
        AccTransaction accTransaction = new AccTransaction();
        accTransaction.setUserId(userId);
        accTransaction.setContractId(contractId);
        accTransaction.setType(result == ContractStatus.SUCCESS ? TransactionType.UNFREEZE : TransactionType.DEDUCT);
        accTransaction.setAmount(deposit);
        accTransaction.setBalanceAfter(user.getBalance());
        accTransaction.setFrozenAfter(user.getFrozen());
        accTransaction.setRemark(result == ContractStatus.SUCCESS ? "履约成功解冻退还" : "违约扣除押金");
        accTransactionMapper.insert(accTransaction);

        int rows = contractMapper.updateStatusWhenVoting(contractId,result.name());
        if (rows == 0){
            throw new BizException("合约状态已被修改，结算中止", 409);
        }

        //结算通知
        String resultLabel = (result == ContractStatus.SUCCESS) ? "已履约成功" : "已违约";
        String settledTitle = "「" + betContract.getTitle() + "」" + resultLabel;
        noticeService.sendNotification(userId, settledTitle, contractId, NotificationType.CONTRACT_SETTLED);
        List<BetContractWitness> witnesses = betContractWitnessMapper.selectList(
                new LambdaQueryWrapper<BetContractWitness>()
                        .eq(BetContractWitness::getContractId, contractId)
        );
        for (BetContractWitness w : witnesses) {
            noticeService.sendNotification(w.getUserId(), settledTitle, contractId, NotificationType.CONTRACT_SETTLED);
        }
    }

    @Override
    public void expireScan() {
        LambdaQueryWrapper<BetContract> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(BetContract::getStatus,ContractStatus.ACTIVE);
        queryWrapper.lt(BetContract::getDeadline, LocalDateTime.now());
        queryWrapper.last("LIMIT 100");

        List<BetContract> betContractList = contractMapper.selectList(queryWrapper);
        for (BetContract betContract : betContractList) {
            expireOne(betContract);
        }
    }

    private void expireOne(BetContract betContract) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            int rows = contractMapper.activeTOVoting(betContract.getId());
            if(rows ==0){
                return;
            }
            LambdaQueryWrapper<BetContractWitness> queryWrapper = Wrappers.lambdaQuery();
            queryWrapper.eq(BetContractWitness::getContractId,betContract.getId());
            List<BetContractWitness> witnessList = betContractWitnessMapper.selectList(queryWrapper);
            for (BetContractWitness betContractWitness : witnessList) {
                SysNotification sysNotification = new SysNotification();
                sysNotification.setType(NotificationType.VERDICT_REQUEST);
                sysNotification.setContractId(betContractWitness.getContractId());
                sysNotification.setUserId(betContractWitness.getUserId());
                sysNotification.setTitle("「" + betContract.getTitle() + "」已到期，请你裁决");
                sysNotificationMapper.insert(sysNotification);
            }
        });


    }

    @Override
    public void timeoutSettle() {
        LambdaQueryWrapper<BetContract> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(BetContract::getStatus,ContractStatus.VOTING);
        List<BetContract> betContractList = contractMapper.selectList(queryWrapper);
        for (BetContract betContract : betContractList) {
            Duration duration = Duration.between(betContract.getDeadline(),LocalDateTime.now());
            if (duration.toHours()>24){
                settle(betContract.getId(), ContractStatus.FAILED);
            }
        }
    }

    @Override
    public void viewCountFlush() {
        Map<Object,Object> counts = stringRedisTemplate.opsForHash().entries("square:view:counts");
        if (counts.isEmpty()){
            return;
        }
        for (Map.Entry<Object,Object> entry : counts.entrySet()){
            BetContract update = new BetContract();
            update.setId(Long.valueOf(entry.getKey().toString()));
            update.setViewCount(Integer.valueOf(entry.getValue().toString()));
            contractMapper.updateById(update);
        }
    }
}
