package com.spring.teamwork.Service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spring.teamwork.Mapper.BetContractWitnessMapper;
import com.spring.teamwork.Mapper.ContractMapper;
import com.spring.teamwork.Mapper.SysUserMapper;
import com.spring.teamwork.Service.SquareService;
import com.spring.teamwork.model.DTO.SquareQueryRequest;
import com.spring.teamwork.model.Entity.BetContract;
import com.spring.teamwork.model.Entity.SysUser;
import com.spring.teamwork.model.PageVO;
import com.spring.teamwork.model.VO.SquareContractItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service@Slf4j@RequiredArgsConstructor
public class SquareServiceImpl implements SquareService {
    private static final String VIEW_HASH_KEY = "square:view:counts";
    private static final String VIEW_DEDUP_PREFIX = "square:view:dedup:";
    private static final Duration DEDUP_TTL = Duration.ofHours(1);
    private  final ContractMapper contractMapper;
    private final BetContractWitnessMapper betContractWitnessMapper;
    private final SysUserMapper sysUserMapper;
    private final StringRedisTemplate stringRedisTemplate;
    @Override
    public PageVO<SquareContractItemVO> pagePublicContracts(SquareQueryRequest req) {
        LambdaQueryWrapper<BetContract> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getKeyword())) {
            qw.like(BetContract::getTitle, req.getKeyword().trim());
        }
        if (req.getCategory() != null) {
            qw.eq(BetContract::getCategory, req.getCategory());
        }
        if (req.getStatus() != null) {
            qw.eq(BetContract::getStatus, req.getStatus());
        }
        qw.eq(BetContract::getIsDeleted, 0);
        qw.orderByDesc(BetContract::getCreatedAt);

        Page<BetContract> contractPage = contractMapper.selectPage(
                new Page<>(req.getPage(), req.getSize()), qw);
        List<SquareContractItemVO> records = new ArrayList<>();
        for (BetContract c : contractPage.getRecords()) {
            SysUser owner = sysUserMapper.selectById(c.getUserId());
            if (owner == null) continue;


            int witnessCount = betContractWitnessMapper.countByContractId(c.getId());


            int viewCount = getViewCount(c.getId());

            records.add(SquareContractItemVO.builder()
                    .contractId(c.getId())
                    .title(c.getTitle())
                    .category(c.getCategory())
                    .status(c.getStatus())
                    .nickname(owner.getNickname())
                    .witnessCount(witnessCount)
                    .viewCount(viewCount)
                    .deadline(c.getDeadline())
                    .createdAt(c.getCreatedAt())
                    .build());
        }
        return new PageVO<>(contractPage.getTotal(), contractPage.getCurrent(),
                contractPage.getSize(), records);

        }

    @Override
    public void incrementViewCount(Long contractId, Long userId, String anonymousKey) {

        String dedupIdentity = userId != null
                ? ("uid:" + userId)
                : ("anon:" + anonymousKey);

        String dedupKey = VIEW_DEDUP_PREFIX + contractId + ":" + dedupIdentity;


        Boolean isNew = stringRedisTemplate.opsForValue().setIfAbsent(
                dedupKey, "1", DEDUP_TTL);

        if (isNew != null && isNew) {

            stringRedisTemplate.opsForHash().increment(VIEW_HASH_KEY,
                    String.valueOf(contractId), 1);
            log.debug("[square] view count +1 contractId={} identity={}", contractId, dedupIdentity);
        }

    }

    @Override
    public int getViewCount(Long contractId) {
        Object val = stringRedisTemplate.opsForHash().get(
                VIEW_HASH_KEY, String.valueOf(contractId));
        return val == null ? 0 : Integer.parseInt(val.toString());

    }
}
