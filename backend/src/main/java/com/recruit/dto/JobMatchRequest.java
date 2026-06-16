package com.recruit.dto;

import lombok.Data;

/**
 * 人岗匹配请求
 */
@Data
public class JobMatchRequest {
    private Long jobId;
    private Long studentId;  // 可选，不传则匹配所有学生
}
