package com.spring.teamwork.model.VO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BalanceVO {
    private Integer balance;
    private Integer frozen;
    private Integer totalDeducted;
}
