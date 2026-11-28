package com.spring.teamwork.model.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 合约-见证人关联表。
 * 注意：不继承 BaseEntity——本表只有 created_at 没有 updated_at（数据库设计文档 2.3），
 * 继承会由 MyBatis-Plus 生成 updated_at 列导致 SQL 报错
 */
@Data
@TableName("bet_contract_witness")
public class BetContractWitness {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long contractId;

    private Long userId;

    private LocalDateTime acceptedAt;

    private LocalDateTime createdAt;
}
