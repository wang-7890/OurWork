package com.spring.teamwork.Service.impl;

import com.spring.teamwork.Exception.common.BizException;
import com.spring.teamwork.Mapper.*;
import com.spring.teamwork.Service.DemoService;
import com.spring.teamwork.Service.NoticeService;
import com.spring.teamwork.Service.SettlementService;
import com.spring.teamwork.model.Entity.BetContract;
import com.spring.teamwork.model.Entity.BetContractWitness;
import com.spring.teamwork.model.Entity.SysUser;
import com.spring.teamwork.model.Enum.ContractCategory;
import com.spring.teamwork.model.Enum.ContractStatus;
import com.spring.teamwork.model.Enum.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service@Slf4j@RequiredArgsConstructor
public class DemoServiceImpl implements DemoService {
    private final ContractMapper contractMapper;
    private final BetContractWitnessMapper betContractWitnessMapper;
    private final BetVoteMapper betVoteMapper;
    private final SysUserMapper sysUserMapper;
    private final AccTransactionMapper accTransactionMapper;
    private final SysNotificationMapper sysNotificationMapper;

    private final SettlementService settlementService;
    private final NoticeService noticeService;

    @Override
    public void expireContract(Long contractId, Long operatorUserId) {
        BetContract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BizException("合约不存在", 404);
        }
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BizException("合约不在进行中（ACTIVE），无法触发到期", 409);
        }
        int rows = contractMapper.activeTOVoting(contractId);
        if (rows == 0) {
            throw new BizException("合约已被他人操作，状态变更失败", 409);
        }
        List<BetContractWitness> witnesses =
                betContractWitnessMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.spring.teamwork.model.Entity.BetContractWitness>()
                                .eq(com.spring.teamwork.model.Entity.BetContractWitness::getContractId, contractId)
                );

        for (var witness : witnesses) {
            noticeService.sendNotification(
                    witness.getUserId(),
                    "「" + contract.getTitle() + "」已到期，请及时裁决",
                    contractId,
                    NotificationType.WITNESS_ACCEPTED
            );
        }

        log.info("[demo] 触发合约立即到期 contractId={} operator={} 通知 {} 个见证人",
                contractId, operatorUserId, witnesses.size());

}

    @Override
    @Transactional(rollbackFor=Exception.class)
    public void resetAll() {
        log.warn("[demo] ⚠️ 一键重置被触发！所有业务数据将被清空...");
        betVoteMapper.delete(null);
        betContractWitnessMapper.delete(null);
        sysNotificationMapper.delete(null);
        accTransactionMapper.delete(null);
        contractMapper.delete(null);
        sysUserMapper.delete(null);
        String[][] users = {
                {"demo_alice", "123456", "爱丽丝"},
                {"demo_bob",   "123456", "鲍勃"},
                {"demo_carol", "123456", "卡罗尔"},
                {"demo_dave",  "123456", "戴夫"},
        };

        Long[] userIds = new Long[users.length];
        for (int i = 0; i < users.length; i++) {
            SysUser user = new SysUser();
            user.setUsername(users[i][0]);
            user.setPassword(new BCryptPasswordEncoder().encode(users[i][1]));
            user.setNickname(users[i][2]);
            user.setBalance(1000);   // 初始 1000 币
            user.setFrozen(0);
            user.setIsDeleted(0);
            sysUserMapper.insert(user);
            userIds[i] = user.getId();
            log.info("[demo] 创建用户 {} id={}", users[i][0], userIds[i]);
        }
        BetContract c1 = new BetContract();
        c1.setUserId(userIds[0]);
        c1.setTitle("Demo：学习对赌（进行中）");
        c1.setCategory(ContractCategory.STUDY);
        c1.setDescription("7 天内读完《深入理解计算机系统》");
        c1.setDeposit(100);
        c1.setDeadline(LocalDateTime.now().plusDays(7));
        c1.setStatus(ContractStatus.ACTIVE);
        c1.setIsDeleted(0);
        contractMapper.insert(c1);
        BetContract c2 = new BetContract();
        c2.setUserId(userIds[0]);
        c2.setTitle("Demo：健身对赌（已到期待裁决）");
        c2.setCategory(ContractCategory.FITNESS);
        c2.setDescription("30 天减 5 公斤");
        c2.setDeposit(200);
        c2.setDeadline(LocalDateTime.now().minusDays(1));
        c2.setStatus(ContractStatus.VOTING);
        c2.setIsDeleted(0);
        contractMapper.insert(c2);
        for (int i = 1; i <= 3; i++) {
            BetContractWitness w = new BetContractWitness();
            w.setContractId(c2.getId());
            w.setUserId(userIds[i]);
            w.setAcceptedAt(LocalDateTime.now());
            betContractWitnessMapper.insert(w);
        }
        log.info("[demo] 重置完成，预置 {} 个用户 + {} 个合约（其中 {} 个已加见证人）",
                users.length, 2, 1);
    }
    @Override
    public void triggerSettleJobs() {
        log.info("[demo] 手动触发结算扫描...");
        settlementService.expireScan();
        settlementService.timeoutSettle();
        settlementService.viewCountFlush();
        log.info("[demo] 手动结算扫描完成");

    }
}
