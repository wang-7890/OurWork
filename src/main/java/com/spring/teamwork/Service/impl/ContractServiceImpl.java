package com.spring.teamwork.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring.teamwork.Exception.common.BizException;
import com.spring.teamwork.Mapper.*;
import com.spring.teamwork.Service.ContractService;
import com.spring.teamwork.Service.NoticeService;
import com.spring.teamwork.model.DTO.ContractCreateRequest;
import com.spring.teamwork.model.DTO.ProofRequest;
import com.spring.teamwork.model.Entity.AccTransaction;
import com.spring.teamwork.model.Entity.BetContract;
import com.spring.teamwork.model.Entity.BetContractWitness;
import com.spring.teamwork.model.Entity.SysUser;
import com.spring.teamwork.model.Enum.ContractStatus;
import com.spring.teamwork.model.Enum.NotificationType;
import com.spring.teamwork.model.Enum.TransactionType;
import com.spring.teamwork.model.PageVO;
import com.spring.teamwork.model.Result;
import com.spring.teamwork.model.VO.AcceptInviteVO;
import com.spring.teamwork.model.VO.ContractDetailVO;
import com.spring.teamwork.model.VO.ContractListVO;
import com.spring.teamwork.model.VO.WitnessInviteVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ContractServiceImpl implements ContractService {
    private static final String CODE_CHARS = "abcdefghjkmnpqrstuvwxyz23456789";

    @Autowired
    private ContractMapper contractMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private AccTransactionMapper accTransactionMapper;
    @Autowired
    private BetContractWitnessMapper betContractWitnessMapper;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private NoticeService noticeService;
    @Value("${invite.base-url}")
    private String inviteBaseUrl;
    @Override
    public Result<ContractDetailVO> create(ContractCreateRequest request, Long userId) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return transactionTemplate.execute(status -> doCreate(request, userId));
    }

    @Override
    public Result<PageVO<ContractListVO>> getMyContract(ContractStatus status, Integer page, Integer size, Long userId) {
        LambdaQueryWrapper<BetContract> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(BetContract::getUserId, userId);
        if (status != null) {
            queryWrapper.eq(BetContract::getStatus, status);
        }
        queryWrapper.orderByDesc(BetContract::getCreatedAt);
        //分页查询
        Page<BetContract> betContractPage = contractMapper.selectPage(new Page<>(page, size), queryWrapper);
        //Entity 映射 VO
        List<ContractListVO> records = betContractPage.getRecords().stream()
                .map(betContract -> {
                    ContractListVO vo = new ContractListVO();
                    vo.setContractId(betContract.getId());       // 注意：Entity 叫 id，VO 叫 contractId，名字对不上
                    vo.setTitle(betContract.getTitle());
                    vo.setCategory(betContract.getCategory());
                    vo.setStatus(betContract.getStatus());
                    vo.setDeposit(betContract.getDeposit());
                    vo.setDeadline(betContract.getDeadline());
                    vo.setCreatedAt(betContract.getCreatedAt());
                    vo.setWitnessCount(betContractWitnessMapper.countByContractId(betContract.getId()));
                    return vo;
                }).toList();
        return Result.ok(new PageVO<>(betContractPage.getTotal(), betContractPage.getCurrent(), betContractPage.getSize(), records));
    }

    @Override
    public Result<ContractDetailVO> getContractDetail(Integer contractId) {
        BetContract betContract = contractMapper.selectById(contractId);
        //组装ContractDetailVO（含有contract的部分）
        ContractDetailVO contractDetailVO = new ContractDetailVO();
        contractDetailVO.setContractId(betContract.getId());
        contractDetailVO.setTitle(betContract.getTitle());
        contractDetailVO.setDescription(betContract.getDescription());
        contractDetailVO.setCategory(betContract.getCategory());
        contractDetailVO.setStatus(betContract.getStatus());
        contractDetailVO.setDeposit(betContract.getDeposit());
        contractDetailVO.setDeadline(betContract.getDeadline());
        contractDetailVO.setCreatedAt(betContract.getCreatedAt());
        contractDetailVO.setSettledAt(betContract.getSettledAt());
        contractDetailVO.setSettleResult(betContract.getSettleResult());
        contractDetailVO.setViewCount(betContract.getViewCount());
        //组装ContractDetailVO的owner部分，从user表里查
        SysUser user = sysUserMapper.selectById(betContract.getUserId());
        ContractDetailVO.OwnerVO ownerVO = new ContractDetailVO.OwnerVO();
        ownerVO.setUserId(user.getId());
        ownerVO.setNickname(user.getNickname());
        contractDetailVO.setOwner(ownerVO);
        //组装 witness
        List<ContractDetailVO.WitnessVO> witnessVOList = betContractWitnessMapper.getWitnesses(Long.valueOf(contractId));
        contractDetailVO.setWitnesses(witnessVOList);
        //组装 proofVO
        ContractDetailVO.ProofVO proofVO = new ContractDetailVO.ProofVO();
        proofVO.setContent(betContract.getProofContent());
        proofVO.setImages(betContract.getProofImages());
        proofVO.setSubmittedAt(betContract.getProofSubmittedAt());
        contractDetailVO.setProof(proofVO);
        return Result.ok(contractDetailVO);
    }

    @Override
    public Result<WitnessInviteVO> createInviteUrl(Integer contractId, Long userId) {
        BetContract betContract = contractMapper.selectById(contractId);
        if(betContract == null || !betContract.getUserId().equals(userId)){
            throw  new BizException("无权限操作该合约", 403);
        }
        if (betContract.getStatus()!=ContractStatus.ACTIVE){
            throw new BizException("合约不在进行中，无法邀请见证人", 409);
        }
        // 幂等：该合约已有有效邀请码则直接复用（文档 3.4：重复调用返回同一个有效码）
        String ownerKey = "contract:invite:owner:" + contractId;
        String existing = stringRedisTemplate.opsForValue().get(ownerKey);
        if (existing != null) {
            return Result.ok(buildInviteVO(existing));
        }
        // 生成新码，双向存 Redis，TTL 72 小时
        String inviteCode = generateCode();
        stringRedisTemplate.opsForValue().set("contract:invite:" + inviteCode, String.valueOf(contractId), Duration.ofHours(72));
        stringRedisTemplate.opsForValue().set(ownerKey, inviteCode, Duration.ofHours(72));
        return Result.ok(buildInviteVO(inviteCode));
    }

    @Override
    public Result<AcceptInviteVO> accept(String inviteCode,Long userId) {
        String contractId = stringRedisTemplate.opsForValue().get("contract:invite:" + inviteCode);
        if (contractId == null){
            throw new BizException("邀请码不存在或已过期", 404);
        }
        BetContract betContract = contractMapper.selectById(contractId);
        if(betContract == null || betContract.getUserId().equals(userId)){
            throw new BizException("合约不存在或不能见证自己的合约",409);
        }
        if(betContractWitnessMapper.countByContractId(Long.valueOf(contractId)) >=5){
            throw new BizException("见证人已经超过5人",409);
        }
        BetContractWitness betContractWitness = new BetContractWitness();
        betContractWitness.setUserId(userId);
        betContractWitness.setContractId(Long.valueOf(contractId));
        betContractWitness.setAcceptedAt(LocalDateTime.now());
        betContractWitnessMapper.insert(betContractWitness);

        // 通知合约创建者：某用户已接受见证邀请
        SysUser witness = sysUserMapper.selectById(userId);
        noticeService.sendNotification(
                betContract.getUserId(),                           // 通知谁 → 合约创建者
                (witness != null ? witness.getNickname() : "用户") + " 已接受你的见证邀请",
                betContract.getId(),                                // 关联哪个合约
                NotificationType.WITNESS_ACCEPTED                   // 什么类型
        );

        AcceptInviteVO acceptInviteVO = new AcceptInviteVO();
        acceptInviteVO.setContractId(Integer.valueOf(contractId));
        acceptInviteVO.setContractTitle(betContract.getTitle());
        return Result.ok(acceptInviteVO);
    }

    private WitnessInviteVO buildInviteVO(String inviteCode) {
        WitnessInviteVO vo = new WitnessInviteVO();
        vo.setInviteCode(inviteCode);
        vo.setInviteUrl(inviteBaseUrl + "/witness/accept/" + inviteCode);
        vo.setExpireHours(72);
        return vo;
    }
    private String generateCode(){
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 提交履约凭证（接口文档 3.6）：仅创建者；ACTIVE 或 VOTING 可提交/覆盖；不影响状态流转
     */
    @Override
    public Result<ContractDetailVO.ProofVO> submitProof(Long contractId, ProofRequest request, Long userId) {
        BetContract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BizException("合约不存在", 404);
        }
        if (!contract.getUserId().equals(userId)) {
            throw new BizException("仅创建者可提交凭证", 403);
        }
        if (contract.getStatus() != ContractStatus.ACTIVE && contract.getStatus() != ContractStatus.VOTING) {
            throw new BizException("合约已完结，无法提交凭证", 409);
        }

        // 覆盖式更新三个凭证字段；proof_images 由 JacksonTypeHandler 自动转 JSON
        BetContract update = new BetContract();
        update.setId(contract.getId());
        update.setProofContent(request.getContent());
        update.setProofImages(request.getImages());
        update.setProofSubmittedAt(LocalDateTime.now());
        contractMapper.updateById(update);

        // 返回更新后的凭证（结构同 3.3 详情页的 proof 字段，前端可直接回填）
        ContractDetailVO.ProofVO vo = new ContractDetailVO.ProofVO();
        vo.setContent(update.getProofContent());
        vo.setImages(update.getProofImages());
        vo.setSubmittedAt(update.getProofSubmittedAt());
        return Result.ok(vo);
    }

    private Result<ContractDetailVO> doCreate(ContractCreateRequest request, Long userId) {
        if (sysUserMapper.freezeDeposit(userId, request.getDeposit()) == 0) {
            throw new BizException("余额不足", 409);
        }

        BetContract contract = new BetContract();
        contract.setUserId(userId);
        contract.setTitle(request.getTitle());
        contract.setDescription(request.getDescription());
        contract.setCategory(request.getCategory());
        contract.setDeposit(request.getDeposit());
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setDeadline(request.getDeadline());
        contract.setViewCount(0);
        contractMapper.insert(contract);

        SysUser user = sysUserMapper.selectById(userId);

        //记 FREEZE 流水：金额恒正，方向由 type 决定，balanceAfter/frozenAfter 用于对账
        AccTransaction tx = new AccTransaction();
        tx.setUserId(userId);
        tx.setContractId(contract.getId());
        tx.setType(TransactionType.FREEZE);
        tx.setAmount(request.getDeposit());
        tx.setBalanceAfter(user.getBalance());
        tx.setFrozenAfter(user.getFrozen());
        tx.setRemark("创建合约冻结押金");
        accTransactionMapper.insert(tx);

        return Result.ok(buildVO(contract, user));
    }

    /**
     * 组装 ContractDetailVO
     */
    private ContractDetailVO buildVO(BetContract contract, SysUser user) {
        ContractDetailVO vo = new ContractDetailVO();
        vo.setContractId(contract.getId());
        vo.setTitle(contract.getTitle());
        vo.setDescription(contract.getDescription());
        vo.setCategory(contract.getCategory());
        vo.setStatus(contract.getStatus());
        vo.setDeposit(contract.getDeposit());
        vo.setDeadline(contract.getDeadline());
        vo.setCreatedAt(contract.getCreatedAt());
        vo.setSettledAt(contract.getSettledAt());
        vo.setSettleResult(contract.getSettleResult());

        ContractDetailVO.OwnerVO owner = new ContractDetailVO.OwnerVO();
        owner.setUserId(user.getId());
        owner.setNickname(user.getNickname());
        vo.setOwner(owner);

        vo.setWitnesses(new ArrayList<>());
        vo.setProof(null);
        vo.setViewCount(contract.getViewCount());
        return vo;
    }
}
