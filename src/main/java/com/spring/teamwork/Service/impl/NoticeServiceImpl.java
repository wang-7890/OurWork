package com.spring.teamwork.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spring.teamwork.Mapper.SysNotificationMapper;
import com.spring.teamwork.Service.NoticeService;
import com.spring.teamwork.model.Entity.SysNotification;
import com.spring.teamwork.model.Enum.NotificationType;
import com.spring.teamwork.model.PageVO;
import com.spring.teamwork.model.VO.NoticeUnreadCountVO;
import com.spring.teamwork.model.VO.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final SysNotificationMapper sysNotificationMapper;

    @Override
    public void sendNotification(Long userId, String title, Long contractId, NotificationType type) {
        SysNotification n = new SysNotification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContractId(contractId);
        n.setIsRead(false);
        sysNotificationMapper.insert(n);
    }

    @Override
    public NoticeUnreadCountVO getUnreadCount(Long userId) {
        int count = sysNotificationMapper.countUnread(userId);
        return NoticeUnreadCountVO.builder().count(count).build();
    }

    @Override
    public PageVO<NotificationVO> pageQuery(Long userId, Integer page, Integer size) {
        Page<SysNotification> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysNotification> qw = new LambdaQueryWrapper<>();
        qw.eq(SysNotification::getUserId, userId)
                .orderByDesc(SysNotification::getCreatedAt);   // 最新的排前面

        Page<SysNotification> result = sysNotificationMapper.selectPage(pageParam, qw);

        List<NotificationVO> records = result.getRecords().stream()
                .map(n -> NotificationVO.builder()
                        .notificationId(n.getId())
                        .type(n.getType())
                        .title(n.getTitle())
                        .contractId(n.getContractId())
                        .read(n.getIsRead())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();

        // 直接用 @AllArgsConstructor 构造 PageVO<NotificationVO>，records 是 VO 列表
        return new PageVO<>(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public void markRead(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        sysNotificationMapper.update(null,
                new LambdaUpdateWrapper<SysNotification>()
                        .set(SysNotification::getIsRead, true)
                        .eq(SysNotification::getUserId, userId)
                        .in(SysNotification::getId, ids)
        );
    }

    @Override
    public void markAllRead(Long userId) {
        sysNotificationMapper.update(null,
                new LambdaUpdateWrapper<SysNotification>()
                        .set(SysNotification::getIsRead, true)
                        .eq(SysNotification::getUserId, userId)
        );
    }
}
