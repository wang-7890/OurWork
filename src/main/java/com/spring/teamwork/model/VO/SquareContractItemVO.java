package com.spring.teamwork.model.VO;

import com.spring.teamwork.model.Enum.ContractCategory;
import com.spring.teamwork.model.Enum.ContractStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data@Builder@NoArgsConstructor@AllArgsConstructor
public class SquareContractItemVO {
    private  Long  contractId;
    private String title;
    private ContractCategory category;
    private ContractStatus status;
    private String nickname;         // 创建者昵称（脱敏：userId 不对外暴露）
    private Integer witnessCount;    // 当前见证人数
    private Integer viewCount;       // 围观数（Redis 实时计数）
    private LocalDateTime deadline;
    private LocalDateTime createdAt;


}
