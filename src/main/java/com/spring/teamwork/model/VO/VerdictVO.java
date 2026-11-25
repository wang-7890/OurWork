package com.spring.teamwork.model.VO;

import com.spring.teamwork.model.Enum.Verdict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 裁决模块全部 VO 聚合文件（接口文档 4.1 / 4.2 / 4.3）。
 * 一个文件搞定所有裁决相关响应结构，避免零散。
 */
public class VerdictVO {

    // 列表项
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingItem {
        private Long contractId;
        private String title;
        private Integer deposit;
        private LocalDateTime deadline;

        /** 进入 VOTING 状态的时间（= 合约 updatedAt），用于前端排序"谁最急" */
        private LocalDateTime votingStartedAt;

        private OwnerVO owner;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OwnerVO {
            private Long userId;
            private String nickname;
        }
    }

    //投票响应
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoteResponse {

        /** 是否投票成功 */
        private Boolean voted;

        /** 投票后的当前票数快照 */
        private VoteCount currentVotes;

        /** 本次投票后合约是否已自动结算（达标 / 全员投完） */
        private Boolean settled;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class VoteCount {
            private Integer success;
            private Integer fail;
            private Integer total;
        }
    }

    // 投票结果
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoteResult {

        private Integer success;
        private Integer fail;
        private Integer total;

        /** 最终裁决结果：SUCCESS / FAIL / null（还没结算） */
        private Verdict result;

        /**
         * 投票明细列表。
         * - 合约未完结：detail 为空数组（只返回计票数字，防跟风投票）
         * - 合约完结：返回全部投票人 + 选项 + 评语 + 时间
         */
        private List<VoteDetail> detail;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class VoteDetail {
            private Long userId;
            private String nickname;
            private Verdict verdict;
            private String comment;
            private LocalDateTime votedAt;
        }
    }
}
