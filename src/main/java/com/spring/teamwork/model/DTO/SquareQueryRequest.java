package com.spring.teamwork.model.DTO;

import com.spring.teamwork.model.Enum.ContractCategory;
import com.spring.teamwork.model.Enum.ContractStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
/*广场查询*/
@Data
public class SquareQueryRequest {
    @Min(value = 1, message = "页码最小为1")
    private Integer page  = 1;

    @Min(value = 1, message = "页大小最小为1")
    @Max(value=50,message = "页面最大为50")
    private Integer size = 10;

    @Size(max = 30,message = "搜索关键词最长为30字符")
    private String keyword;

    private ContractStatus status;

    private ContractCategory category;
}
