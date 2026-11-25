package com.spring.teamwork.model.DTO;

import com.spring.teamwork.model.Enum.Verdict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
/*裁决投票*/
@Data
public class VoteRequest {

    @NotNull(message = "投票结果不为空")
    private Verdict verdict;

    @Size(max = 200, message = "评语最长 200 字")
    private String comment;

}
