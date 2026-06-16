package com.recruit.dto;

import lombok.Data;

/**
 * 简历保存/更新请求
 */
@Data
public class ResumeRequest {
    private Long studentId;
    private String education;      // JSON格式
    private String internship;     // JSON格式
    private String skills;         // 技能标签，逗号分隔
    private String selfEvaluation;
    private String jobTarget;     // 求职意向
    private String pdfUrl;        // PDF文件路径
}
