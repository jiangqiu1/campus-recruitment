-- ====================================
-- 职业院校校企招聘与就业管理平台
-- 数据库初始化脚本（MySQL 8.0+）
-- 数据库名称：campus_recruitment
-- 字符集：utf8mb4
-- 存储引擎：InnoDB
-- ====================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `campus_recruitment`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `campus_recruitment`;

-- ====================================
-- 3.1 用户表（sys_user）
-- ====================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`            BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`      VARCHAR(50)  NOT NULL COMMENT '用户名（学号/工号）',
    `password`      VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `real_name`     VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    `role`          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '角色：0=学生，1=教师，2=企业HR，3=管理员',
    `phone`         VARCHAR(20)  DEFAULT NULL COMMENT '手机号（AES加密）',
    `wechat_openid` VARCHAR(100) DEFAULT NULL COMMENT '微信OpenID',
    `avatar_url`    VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `status`        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '状态：0=禁用，1=正常',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       INT(1)        DEFAULT 0 COMMENT '逻辑删除标志：0=未删除，1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_wechat_openid` (`wechat_openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ====================================
-- 3.2 企业表（company）
-- ====================================
DROP TABLE IF EXISTS `company`;
CREATE TABLE `company` (
    `id`                  BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '企业ID',
    `name`                VARCHAR(100)  NOT NULL COMMENT '企业全称',
    `short_name`          VARCHAR(50)   DEFAULT NULL COMMENT '简称',
    `license_url`         VARCHAR(255)  DEFAULT NULL COMMENT '营业执照图片路径',
    `industry`            VARCHAR(50)   DEFAULT NULL COMMENT '行业',
    `address`             VARCHAR(200)  DEFAULT NULL COMMENT '地址',
    `contact_person`      VARCHAR(50)   DEFAULT NULL COMMENT '联系人',
    `contact_phone`       VARCHAR(20)   DEFAULT NULL COMMENT '联系电话（AES加密）',
    `cooperation_level`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '合作等级：0=潜在，1=合作中，2=核心，3=已流失',
    `last_recruit_time`   DATETIME       DEFAULT NULL COMMENT '最近一次招聘时间',
    `create_time`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`             INT(1)         DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业表';

-- ====================================
-- 3.3 岗位表（job）
-- ====================================
DROP TABLE IF EXISTS `job`;
CREATE TABLE `job` (
    `id`               BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
    `company_id`       BIGINT(20)   NOT NULL COMMENT '所属企业ID',
    `title`            VARCHAR(100)  NOT NULL COMMENT '岗位名称',
    `salary_range`     VARCHAR(50)   DEFAULT NULL COMMENT '薪资范围（如"8k-12k"）',
    `education`        VARCHAR(20)   DEFAULT NULL COMMENT '学历要求',
    `location`         VARCHAR(100)  DEFAULT NULL COMMENT '工作地点',
    `description`      TEXT           DEFAULT NULL COMMENT '岗位描述',
    `requirement`      TEXT           DEFAULT NULL COMMENT '任职要求',
    `deadline`         DATE           DEFAULT NULL COMMENT '截止日期',
    `status`           TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '状态：0=草稿，1=已发布，2=已关闭，3=暂停',
    `created_by`       BIGINT(20)    NOT NULL COMMENT '发布者ID（教师或HR）',
    `view_count`       INT(11)       NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `qr_code_url`      VARCHAR(255)  DEFAULT NULL COMMENT '专属二维码图片路径',
    `trace_id`         VARCHAR(32)    NOT NULL COMMENT '唯一追踪ID',
    `create_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `ai_keywords`      VARCHAR(500)  DEFAULT NULL COMMENT '从岗位描述中提取的关键词（JSON）',
    `required_skills`  VARCHAR(500)  DEFAULT NULL COMMENT '所需技能标签（JSON）',
    `deleted`          INT(1)         DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trace_id` (`trace_id`),
    KEY `idx_company_id` (`company_id`),
    KEY `idx_status_deadline` (`status`, `deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位表';

-- ====================================
-- 3.4 学生简历表（resume）
-- ====================================
DROP TABLE IF EXISTS `resume`;
CREATE TABLE `resume` (
    `id`            BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '简历ID',
    `student_id`    BIGINT(20)   NOT NULL COMMENT '学生ID',
    `education`     TEXT           DEFAULT NULL COMMENT '教育经历（JSON）',
    `internship`    TEXT           DEFAULT NULL COMMENT '实习经历（JSON）',
    `skills`        VARCHAR(500)  DEFAULT NULL COMMENT '技能证书',
    `self_evaluation` TEXT         DEFAULT NULL COMMENT '自我评价',
    `pdf_url`       VARCHAR(255)  DEFAULT NULL COMMENT '上传的PDF简历路径',
    `is_default`    TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '是否为当前默认简历：0=否，1=是',
    `update_time`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `skill_tags`    VARCHAR(500)  DEFAULT NULL COMMENT '从简历中提取的技能标签（JSON）',
    `job_target`    VARCHAR(500)  DEFAULT NULL COMMENT '求职意向',
    `deleted`       INT(1)         DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生简历表';

-- ====================================
-- 3.5 投递记录表（delivery）
-- ====================================
DROP TABLE IF EXISTS `delivery`;
CREATE TABLE `delivery` (
    `id`              BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '投递ID',
    `student_id`      BIGINT(20)   NOT NULL COMMENT '学生ID',
    `job_id`          BIGINT(20)   NOT NULL COMMENT '岗位ID',
    `resume_version`  VARCHAR(50)  NOT NULL COMMENT '投递时的简历版本快照',
    `status`          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '状态：0=已投递，1=企业已查看，2=待面试，3=已录用，4=不合适',
    `interview_time`  DATETIME       DEFAULT NULL COMMENT '面试时间',
    `interview_location` VARCHAR(200) DEFAULT NULL COMMENT '面试地点',
    `feedback`        VARCHAR(500)  DEFAULT NULL COMMENT '企业反馈',
    `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投递时间',
    `deleted`         INT(1)         DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投递记录表';

-- ====================================
-- 3.6 班级表（class）
-- ====================================
DROP TABLE IF EXISTS `class`;
CREATE TABLE `class` (
    `id`          BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '班级ID',
    `name`        VARCHAR(50)  NOT NULL COMMENT '班级名称',
    `teacher_id`  BIGINT(20)   NOT NULL COMMENT '班主任/教师ID',
    `major`       VARCHAR(50)   DEFAULT NULL COMMENT '专业名称',
    `grade`       VARCHAR(10)   DEFAULT NULL COMMENT '年级（如2023级）',
    `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`     INT(1)        DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    KEY `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级表';

-- ====================================
-- 3.7 学生-班级关联表（student_class）
-- ====================================
DROP TABLE IF EXISTS `student_class`;
CREATE TABLE `student_class` (
    `id`         BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id` BIGINT(20) NOT NULL COMMENT '学生ID',
    `class_id`   BIGINT(20) NOT NULL COMMENT '班级ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_class` (`student_id`, `class_id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生-班级关联表';

-- ====================================
-- 3.8 企业-岗位变更申请记录表（job_change_apply）
-- ====================================
DROP TABLE IF EXISTS `job_change_apply`;
CREATE TABLE `job_change_apply` (
    `id`               BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    `job_id`           BIGINT(20) NOT NULL COMMENT '岗位ID',
    `hr_id`            BIGINT(20) NOT NULL COMMENT '申请HR ID',
    `change_content`    TEXT        NOT NULL COMMENT '变更内容（JSON）',
    `status`           TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '状态：0=待审核，1=通过，2=拒绝',
    `review_teacher_id` BIGINT(20)  DEFAULT NULL COMMENT '审核教师ID',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_hr_id` (`hr_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业-岗位变更申请记录表';

-- ====================================
-- 3.9 操作日志表（operation_log）
-- ====================================
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
    `id`             BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id`        BIGINT(20)   NOT NULL COMMENT '操作用户ID',
    `operation_type`  VARCHAR(50)  NOT NULL COMMENT '操作类型（查看简历、下载等）',
    `target_id`      VARCHAR(100)  DEFAULT NULL COMMENT '目标对象ID（如简历ID）',
    `ip_address`     VARCHAR(50)   DEFAULT NULL COMMENT '操作IP',
    `create_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id_create_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ====================================
-- 3.10 AI解析日志表（ai_parse_log）
-- ====================================
DROP TABLE IF EXISTS `ai_parse_log`;
CREATE TABLE `ai_parse_log` (
    `id`                  BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `teacher_id`          BIGINT(20)   NOT NULL COMMENT '发起解析的教师ID',
    `raw_message`        TEXT          NOT NULL COMMENT '原始转发消息（文本或图片描述）',
    `parsed_result`       TEXT          NOT NULL COMMENT 'AI解析输出的结构化JSON',
    `is_manual_corrected` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否人工修正：0=未修正，1=已人工修正',
    `corrected_result`    TEXT           DEFAULT NULL COMMENT '人工修正后的JSON',
    `confidence_score`    DECIMAL(5,2)  DEFAULT NULL COMMENT 'AI整体置信度（0-1）',
    `create_time`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '解析时间',
    PRIMARY KEY (`id`),
    KEY `idx_teacher_id_create_time` (`teacher_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI解析日志表';

-- ====================================
-- 3.11 人岗匹配记录表（job_match_record）
-- ====================================
DROP TABLE IF EXISTS `job_match_record`;
CREATE TABLE `job_match_record` (
    `id`           BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `job_id`       BIGINT(20)   NOT NULL COMMENT '岗位ID',
    `student_id`   BIGINT(20)   NOT NULL COMMENT '学生ID',
    `match_score`  DECIMAL(5,2) NOT NULL COMMENT '匹配度分数（0-1）',
    `match_reason` VARCHAR(255)  DEFAULT NULL COMMENT '匹配理由（如"技能匹配：Java,Spring"）',
    `is_pushed`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否推送：0=未推送，1=已推送',
    `push_time`    DATETIME       DEFAULT NULL COMMENT '推送时间',
    `is_clicked`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否点击：0=未点击，1=已点击',
    `create_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '计算时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id_match_score` (`job_id`, `match_score`),
    KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人岗匹配记录表';

-- ====================================
-- 3.12 简历智能评分记录表（resume_score_log）
-- ====================================
DROP TABLE IF EXISTS `resume_score_log`;
CREATE TABLE `resume_score_log` (
    `id`           BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `job_id`       BIGINT(20)   NOT NULL COMMENT '岗位ID',
    `delivery_id`  BIGINT(20)   NOT NULL COMMENT '投递记录ID',
    `score`        INT(11)       NOT NULL COMMENT '总分（如0-100）',
    `score_detail` TEXT           DEFAULT NULL COMMENT '评分细则JSON（如"技能得分90，经验得分70"）',
    `create_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id_score` (`job_id`, `score`),
    KEY `idx_delivery_id` (`delivery_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历智能评分记录表';

-- ====================================
-- 3.13 AI反馈日志表（ai_feedback_log）
-- ====================================
DROP TABLE IF EXISTS `ai_feedback_log`;
CREATE TABLE `ai_feedback_log` (
    `id`              BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id`         BIGINT(20)   NOT NULL COMMENT '用户ID',
    `feedback_type`    VARCHAR(50)  NOT NULL COMMENT '反馈类型：parse_error / match_bad / match_good',
    `target_id`       BIGINT(20)   NOT NULL COMMENT '目标对象ID（如简历ID、岗位ID）',
    `feedback_content` VARCHAR(500)  DEFAULT NULL COMMENT '反馈内容',
    `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_target_id` (`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI反馈日志表';

-- ====================================
-- 外键约束（可选，根据性能需求决定是否启用）
-- ====================================
-- ALTER TABLE `job` ADD CONSTRAINT `fk_job_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`id`);
-- ALTER TABLE `job` ADD CONSTRAINT `fk_job_creator` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`);
-- ALTER TABLE `resume` ADD CONSTRAINT `fk_resume_student` FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`id`);
-- ALTER TABLE `delivery` ADD CONSTRAINT `fk_delivery_student` FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`id`);
-- ALTER TABLE `delivery` ADD CONSTRAINT `fk_delivery_job` FOREIGN KEY (`job_id`) REFERENCES `job` (`id`);
-- ALTER TABLE `class` ADD CONSTRAINT `fk_class_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `sys_user` (`id`);
-- ALTER TABLE `student_class` ADD CONSTRAINT `fk_sc_student` FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`id`);
-- ALTER TABLE `student_class` ADD CONSTRAINT `fk_sc_class` FOREIGN KEY (`class_id`) REFERENCES `class` (`id`);

-- ====================================
-- 完成提示
-- ====================================
SELECT '数据库初始化完成！' AS message;
