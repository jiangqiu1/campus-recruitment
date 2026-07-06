package com.recruit.dto;

import lombok.Data;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 投递状态更新请求
 */
@Data
public class DeliveryStatusUpdateRequest {
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值范围为0-4")
    @Max(value = 4, message = "状态值范围为0-4")
    private Integer status;

    private String feedback;
}
