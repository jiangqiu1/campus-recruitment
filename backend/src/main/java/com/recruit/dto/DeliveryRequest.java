package com.recruit.dto;

import lombok.Data;

/**
 * 投递简历请求
 */
@Data
public class DeliveryRequest {
    private Long studentId;
    private Long jobId;
    private String resumeVersion;  // 简历版本
}
