package com.spring.teamwork.Service;

import com.spring.teamwork.model.Enum.NotificationType;
import com.spring.teamwork.model.PageVO;
import com.spring.teamwork.model.VO.NoticeUnreadCountVO;
import com.spring.teamwork.model.VO.NotificationVO;

import java.util.List;

public interface NoticeService {
    /**
     * 新增通知（内部方法，给 ContractService / WitnessService / 定时任务调用）。
     * 单次创建，存 MySQL，由 MyBatis-Plus 自动填充 createdAt。
     */
    NoticeUnreadCountVO getUnreadCount(Long userId);

    /** 分页查询我的通知（接口文档 8.2） */
    PageVO<NotificationVO> pageQuery(Long userId, Integer page, Integer size);

    /** 批量标记已读（接口文档 8.3：ids 数组） */
    void markRead(Long userId, List<Long> ids);

    /** 全部标记已读（接口文档 8.3：all=true 或 ids=[]） */
    void markAllRead(Long userId);

    void sendNotification(Long userId, String title, Long contractId, NotificationType type);
}
