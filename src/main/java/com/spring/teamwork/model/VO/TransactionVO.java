package com.spring.teamwork.model.VO;

import com.spring.teamwork.model.Enum.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionVO {
    private Long transactionId;
    private TransactionType type;
    private Integer amount;
    private Integer balanceAfter;
    private Long contractId;
    private String contractTitle;   // 联查 bet_contract 填充
    private String remark;
    private LocalDateTime createdAt;
}
