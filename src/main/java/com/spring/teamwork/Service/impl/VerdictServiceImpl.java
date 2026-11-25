package com.spring.teamwork.Service.impl;

import com.spring.teamwork.Service.VerdictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spring.teamwork.common.SettleRemoteClient;
import com.spring.teamwork.Exception.common.BizException;
import com.spring.teamwork.Mapper.*;
import com.spring.teamwork.Service.NoticeService;
import com.spring.teamwork.Service.VerdictService;
import com.spring.teamwork.model.Entity.BetContract;
import com.spring.teamwork.model.Entity.BetContractWitness;
import com.spring.teamwork.model.Entity.BetVote;
import com.spring.teamwork.model.Enum.ContractStatus;
import com.spring.teamwork.model.Enum.NotificationType;
import com.spring.teamwork.model.Enum.Verdict;
import com.spring.teamwork.model.PageVO;
import com.spring.teamwork.model.VO.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j@Service@RequiredArgsConstructor
public class VerdictServiceImpl implements VerdictService {
    /** 投票幂等键 TTL（覆盖结算完成 + 定时兜底的窗口） */
    private static final Duration IDEM_TTL = Duration.ofHours(48);

    private final BetVoteMapper betVoteMapper;
    private final BetContractWitnessMapper betContractWitnessMapper;
    private final ContractMapper contractMapper;
    private final SysUserMapper sysUserMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final NoticeService noticeService;
    private final SettleRemoteClient settleRemoteClient;
    @Override
    public PageVO<VerdictVO.PendingItem> pagePending(Long userId, Integer page, Integer size) {


        List<BetContractWitness> myWitnesses = betContractWitnessMapper.selectList(
                new LambdaQueryWrapper<BetContractWitness>()
                        .eq(BetContractWitness::getUserId, userId)
        );
        if (myWitnesses.isEmpty()) {
            return new PageVO<>(0L, (long) page, (long) size, new ArrayList<>());
        }
        Set<Long> witnessContractIds = myWitnesses.stream()
                .map(BetContractWitness::getContractId).collect(Collectors.toSet());


        LambdaQueryWrapper<BetContract> contractQw = new LambdaQueryWrapper<>();
        contractQw.in(BetContract::getId, witnessContractIds)
                .eq(BetContract::getStatus, ContractStatus.VOTING)
                .orderByAsc(BetContract::getUpdatedAt);  // 最早进入 VOTING 的排最前 → 最急

        Page<BetContract> contractPage = contractMapper.selectPage(new Page<>(page, size), contractQw);


        //组装 VO
        List<VerdictVO.PendingItem> records = new ArrayList<>();
        for (BetContract c : contractPage.getRecords()) {
            //  检查我是否已投
            int myVoteCount = betVoteMapper.countByContractIdAndUserId(c.getId(), userId);
            if (myVoteCount > 0) continue;   // 已投 → 跳过

            // 查创建者昵称
            var owner = sysUserMapper.selectById(c.getUserId());
            if (owner == null) continue;

            // votingStartedAt 用 updatedAt（合约流转到 VOTING 时 updatedAt 被更新）
            records.add(VerdictVO.PendingItem.builder()
                    .contractId(c.getId())
                    .title(c.getTitle())
                    .deposit(c.getDeposit())
                    .deadline(c.getDeadline())
                    .votingStartedAt(c.getUpdatedAt())
                    .owner(VerdictVO.PendingItem.OwnerVO.builder()
                            .userId(owner.getId())
                            .nickname(owner.getNickname())
                            .build())
                    .build());
        }

        return new PageVO<>(contractPage.getTotal(), contractPage.getCurrent(),
                contractPage.getSize(), records);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public VerdictVO.VoteResponse submitVote(Long contractId, Verdict verdict, String comment, Long userId) {

        // ---- 前置校验 ----
        BetContract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BizException("合约不存在", 404);
        }

        // 仅 VOTING 状态可投
        if (contract.getStatus() != ContractStatus.VOTING) {
            throw new BizException("合约不在待裁决状态，无法投票", 409);
        }

        // 仅该合约的见证人可投
        Long witnessExists = betContractWitnessMapper.selectCount(
                new LambdaQueryWrapper<BetContractWitness>()
                        .eq(BetContractWitness::getContractId, contractId)
                        .eq(BetContractWitness::getUserId, userId)
        );
        if (witnessExists == null || witnessExists == 0) {
            throw new BizException("你不是该合约的见证人，无权投票", 403);
        }

        // ---- 双重幂等 ----
        // 1) Redis SETNX（最快，挡住并发重复提交）
        String idemKey = "vote:idem:" + contractId + ":" + userId;
        Boolean firstTry = stringRedisTemplate.opsForValue().setIfAbsent(idemKey, "1", IDEM_TTL);
        if (firstTry == null || !firstTry) {
            // Redis 幂等命中 → 查 DB 返回已投票的结果（不报错，友好提示）
            BetVote existVote = betVoteMapper.selectOne(
                    new LambdaQueryWrapper<BetVote>()
                            .eq(BetVote::getContractId, contractId)
                            .eq(BetVote::getUserId, userId)
            );
            if (existVote != null) {
                VerdictVO.VoteResponse.VoteCount count = buildVoteCount(contractId);
                return VerdictVO.VoteResponse.builder()
                        .voted(false)
                        .currentVotes(count)
                        .settled(contract.getStatus() == ContractStatus.SUCCESS
                                || contract.getStatus() == ContractStatus.FAILED)
                        .build();
            }
            // Redis 误命中但 DB 里没有 → 回滚 Redis 标记后继续（极端场景）
            stringRedisTemplate.delete(idemKey);
        }

        // 2) DB 唯一索引兜底（即使 Redis 失效也防重复插入）
        Long dbExist = betVoteMapper.selectCount(
                new LambdaQueryWrapper<BetVote>()
                        .eq(BetVote::getContractId, contractId)
                        .eq(BetVote::getUserId, userId)
        );
        if (dbExist != null && dbExist > 0) {
            throw new BizException("你已经投过票了，不可改票", 409);
        }

        // ---- 写投票记录 ----
        BetVote vote = new BetVote();
        vote.setContractId(contractId);
        vote.setUserId(userId);
        vote.setVerdict(verdict);
        vote.setComment(comment);
        vote.setCreatedAt(java.time.LocalDateTime.now());
        betVoteMapper.insert(vote);

        log.info("[vote] 用户 {} 对合约 {} 投了 {} 票", userId, contractId, verdict);

        // ---- 实时计票 + 判断裁决条件 ----
        VerdictVO.VoteResponse.VoteCount count = buildVoteCount(contractId);
        int totalWitnesses = betContractWitnessMapper.countByContractId(contractId);

        boolean shouldSettle = false;
        Verdict settleResult = null;

        // 条件 A：全员投完 → 必结算
        if (count.getTotal() >= totalWitnesses) {
            shouldSettle = true;
            settleResult = count.getSuccess() > count.getFail() ? Verdict.SUCCESS : Verdict.FAIL;
            // 平票算失败（接口文档 1.4：FAIL 票 ≥ SUCCESS 票 → 合约 FAILED）
        }
        // 条件 B：SUCCESS 票已超过剩余可能票数 → 提前结算（多数已决）
        else if (count.getSuccess() > totalWitnesses - count.getSuccess()) {
            shouldSettle = true;
            settleResult = Verdict.SUCCESS;
        }
        // 条件 C：FAIL 票已超过剩余可能票数 → 提前结算
        else if (count.getFail() > totalWitnesses - count.getFail()) {
            shouldSettle = true;
            settleResult = Verdict.FAIL;
        }

        // ---- 达标：触发甲方结算 ----
        boolean settled = false;
        if (shouldSettle) {
            log.info("[vote] 合约 {} 已达裁决条件 result={}，触发远程结算", contractId, settleResult);

            // 给合约创建者发个"即将结算"的通知（此时合约状态还是 VOTING，让他们有个心理准备）
            String creatorNickname = sysUserMapper.selectById(contract.getUserId()) != null
                    ? sysUserMapper.selectById(contract.getUserId()).getNickname() : "用户";
            noticeService.sendNotification(contract.getUserId(),
                    creatorNickname + " 的合约已全部投票完成，即将自动结算",
                    contractId, NotificationType.CONTRACT_SETTLED);

            // 远程调用甲方结算接口（同步，最多等 3 秒 + 3 次重试）
            settled = settleRemoteClient.triggerSettle(contractId, settleResult);

            // 投票本身不受结算结果影响：投了就是投了
            // 如果结算没成功，定时任务会兜底扫描"VOTING 但已全员投完"的合约
        }

        return VerdictVO.VoteResponse.builder()
                .voted(true)
                .currentVotes(count)
                .settled(settled)
                .build();
    }
    @Override
    public VerdictVO.VoteResult getVoteResult(Long contractId, Long currentUserId) {

        BetContract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BizException("合约不存在", 404);
        }

        VerdictVO.VoteResponse.VoteCount count = buildVoteCount(contractId);
        boolean isFinal = contract.getStatus() == ContractStatus.SUCCESS
                || contract.getStatus() == ContractStatus.FAILED;

        // 未完结：只返回计票数字，detail 为空数组（防止跟风投票）
        // 完结后：公开全部投票明细
        List<VerdictVO.VoteResult.VoteDetail> detail = new ArrayList<>();
        if (isFinal) {
            detail = betVoteMapper.selectVoteDetails(contractId);
        } else {
            // 未完结但允许看自己投的那一条（避免用户误以为自己没投上）
            BetVote myVote = betVoteMapper.selectOne(
                    new LambdaQueryWrapper<BetVote>()
                            .eq(BetVote::getContractId, contractId)
                            .eq(BetVote::getUserId, currentUserId)
            );
            if (myVote != null) {
                var me = sysUserMapper.selectById(currentUserId);
                detail.add(VerdictVO.VoteResult.VoteDetail.builder()
                        .userId(currentUserId)
                        .nickname(me != null ? me.getNickname() : "匿名")
                        .verdict(myVote.getVerdict())
                        .comment(myVote.getComment())
                        .votedAt(myVote.getCreatedAt())
                        .build());
            }
        }

        Verdict result = isFinal ? contract.getSettleResult() : null;

        return VerdictVO.VoteResult.builder()
                .success(count.getSuccess())
                .fail(count.getFail())
                .total(count.getTotal())
                .result(result)
                .detail(detail)
                .build();
    }

    // ==================== 私有：计票 ====================
    private VerdictVO.VoteResponse.VoteCount buildVoteCount(Long contractId) {
        int success = betVoteMapper.countByContractIdAndVerdict(contractId, Verdict.SUCCESS);
        int fail = betVoteMapper.countByContractIdAndVerdict(contractId, Verdict.FAIL);
        return VerdictVO.VoteResponse.VoteCount.builder()
                .success(success)
                .fail(fail)
                .total(success + fail)
                .build();
    }


}
