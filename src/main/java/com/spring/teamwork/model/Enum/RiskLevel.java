package com.spring.teamwork.model.Enum;
/*
 * 判定规则（MVP）：
 *   HIGH   — 累计违约扣款 >= 2000  或  违约率 > 50%
 *   MEDIUM — 违约率 20% ~ 50%
 *   LOW    — 零违约 或 违约率 < 20% 或 无合约记录（新用户默认 LOW）
 */
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}
