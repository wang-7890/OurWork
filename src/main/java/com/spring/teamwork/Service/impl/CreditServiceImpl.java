package com.spring.teamwork.Service.impl;

import com.spring.teamwork.Exception.common.BizException;
import com.spring.teamwork.Mapper.AccTransactionMapper;
import com.spring.teamwork.Mapper.ContractMapper;
import com.spring.teamwork.Mapper.SysUserMapper;
import com.spring.teamwork.Service.CreditService;
import com.spring.teamwork.model.Entity.SysUser;
import com.spring.teamwork.model.Enum.ContractStatus;
import com.spring.teamwork.model.Enum.RiskLevel;
import com.spring.teamwork.model.VO.CreditVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    private final SysUserMapper sysUserMapper;
    private final ContractMapper contractMapper;
    private final AccTransactionMapper accTransactionMapper;

    @Override
    public CreditVO.Profile getMyProfile(Long userId) {
        return buildProfile(userId, true);
    }

    @Override
    public CreditVO.Profile getOtherProfile(Long targetUserId) {
        return buildProfile(targetUserId, false);
    }


    private CreditVO.Profile buildProfile(Long userId, boolean isMe) {

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在", 404);
        }

        int total   = contractMapper.countAllByUserId(userId);
        int success = contractMapper.countByStatus(userId, ContractStatus.SUCCESS);
        int fail    = contractMapper.countByStatus(userId, ContractStatus.FAILED);
        int active  = contractMapper.countByStatus(userId, ContractStatus.ACTIVE)
                    + contractMapper.countByStatus(userId, ContractStatus.VOTING);

        BigDecimal fulfillRate = BigDecimal.ZERO;
        int settled = success + fail;
        if (settled > 0) {
            fulfillRate = BigDecimal.valueOf(success)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(settled), 1, RoundingMode.HALF_UP);
        }

        int totalDeducted = accTransactionMapper.countDeducted(userId);
        RiskLevel riskLevel = calcRiskLevel(fail, settled, totalDeducted);
        List<CreditVO.RecentContract> recent = contractMapper.selectRecent(userId, 5);
        return CreditVO.Profile.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .balance(isMe ? user.getBalance() : null)
                .frozen(isMe ? user.getFrozen() : null)
                .totalContracts(total)
                .successCount(success)
                .failCount(fail)
                .activeCount(active)
                .fulfillRate(fulfillRate)
                .riskLevel(riskLevel)
                .totalDeducted(totalDeducted)
                .recentContracts(recent)
                .build();
    }

    /**
     * 风险等级（MVP 规则，后续可扩展）：
     *   HIGH   — 扣款总额 >= 2000  或  违约率 > 50%
     *   MEDIUM — 违约率 20% ~ 50%
     *   LOW    — 其他（无合约默认 LOW）
     */
    private RiskLevel calcRiskLevel(int fail, int settled, int totalDeducted) {
        if (totalDeducted >= 2000) {
            return RiskLevel.HIGH;
        }
        if (settled == 0) {
            return RiskLevel.LOW;
        }
        double failRate = (double) fail / settled;
        if (failRate > 0.5) {
            return RiskLevel.HIGH;
        }
        if (failRate >= 0.2) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }
}
