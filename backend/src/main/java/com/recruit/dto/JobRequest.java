package com.recruit.dto;

import lombok.Data;

/**
 * 岗位创建/更新请求
 */
@Data
public class JobRequest {
    private Long companyId;
    private String title;
    private String salaryRange;
    private String education;
    private String location;
    private String description;
    private String requirement;
    private String deadline;  // YYYY-MM-DD
    private Integer status;   // 0=草稿，1=发布
}
