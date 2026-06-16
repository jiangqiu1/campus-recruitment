package com.recruit.dto;

import lombok.Data;

/**
 * 企业创建/更新请求
 */
@Data
public class CompanyRequest {
    private String name;
    private String shortName;
    private String industry;
    private String address;
    private String contactPerson;
    private String contactPhone;  // 会加密存储
    private Integer cooperationLevel;
}
