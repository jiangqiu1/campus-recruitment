package com.recruit.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 岗位变更申请请求
 */
@Data
public class JobChangeSubmitRequest {
    @NotNull(message = "岗位ID不能为空")
    private Long jobId;

    @NotNull(message = "HR ID不能为空")
    private Long hrId;

    @NotBlank(message = "变更内容不能为空")
    private String changeContent;
}
