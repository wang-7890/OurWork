package com.spring.teamwork.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spring.teamwork.model.Entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 原子冻结押金：余额足够才冻结，影响行数为 0 表示余额不足
     * 对应数据库设计文档 4.2，严禁先 SELECT 余额再 UPDATE
     */
    @Update("UPDATE sys_user SET balance = balance - #{deposit}, frozen = frozen + #{deposit} " +
            "WHERE id = #{userId} AND balance >= #{deposit}")
    int freezeDeposit(@Param("userId") Long userId, @Param("deposit") Integer deposit);
    @Update("UPDATE sys_user SET balance = balance+#{deposit},frozen = frozen - #{deposit} WHERE id = #{userId}" +
            "AND frozen >= #{deposit}")
    void unfreezeDeposit(@Param("userId") Long userId,@Param("deposit") Integer deposit);
    @Update("UPDATE sys_user SET frozen = frozen - #{deposit} WHERE id = #{userId}" +
            "AND frozen >=#{deposit}")
    void deductDeposit(@Param("userId") Long userId,@Param("deposit") Integer deposit);
}
