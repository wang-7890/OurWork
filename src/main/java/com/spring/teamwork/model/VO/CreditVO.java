package com.spring.teamwork.model.VO;

import com.spring.teamwork.model.Enum.ContractStatus;
import com.spring.teamwork.model.Enum.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 诚信档案模块全部 VO 聚合（接口文档 6.1 + 6.2）。
 */
public class CreditVO {
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Profile {
        private Long userId;
        private String username;
        private String nickname;
        private String avatar;

        private Integer balance;
        private Integer frozen;
        private Integer totalContracts;
        private Integer successCount;
        private Integer failCount;
        private Integer activeCount;

        private BigDecimal fulfillRate;

        private RiskLevel riskLevel;
        private Integer totalDeducted;

        private List<RecentContract> recentContracts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentContract {
        private Long contractId;
        private String title;
        private ContractStatus status;
        private Integer deposit;
        private LocalDateTime deadline;
        private LocalDateTime createdAt;
    }

}
