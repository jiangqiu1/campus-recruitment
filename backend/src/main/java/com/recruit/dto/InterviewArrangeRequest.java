package com.recruit.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 面试安排请求
 */
@Data
public class InterviewArrangeRequest {
    @NotNull(message = "面试时间不能为空")
    private LocalDateTime interviewTime;

    @NotBlank(message = "面试地点不能为空")
    private String interviewLocation;
}
