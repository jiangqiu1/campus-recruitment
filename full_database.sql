-- ====================================
-- 职业院校校企招聘与就业管理平台
-- 完整数据库初始化脚本（schema + data + 补充表）
-- 数据库名称：campus_recruitment
-- MySQL 8.0+ / utf8mb4 / InnoDB
-- ====================================

CREATE DATABASE IF NOT EXISTS `campus_recruitment`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `campus_recruitment`;

-- ====================================
-- 1. 用户表
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
    `company_id`    BIGINT(20)   DEFAULT NULL COMMENT '关联企业ID（仅HR角色使用）',
    `status`        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '状态：0=禁用，1=正常',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       INT(1)       DEFAULT 0 COMMENT '逻辑删除标志：0=未删除，1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_wechat_openid` (`wechat_openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ====================================
-- 2. 企业表
-- ====================================
DROP TABLE IF EXISTS `company`;
CREATE TABLE `company` (
    `id`                  BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '企业ID',
    `name`                VARCHAR(100) NOT NULL COMMENT '企业全称',
    `short_name`          VARCHAR(50)  DEFAULT NULL COMMENT '简称',
    `license_url`         VARCHAR(255) DEFAULT NULL COMMENT '营业执照图片路径',
    `industry`            VARCHAR(50)  DEFAULT NULL COMMENT '行业',
    `address`             VARCHAR(200) DEFAULT NULL COMMENT '地址',
    `contact_person`      VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
    `contact_phone`       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话（AES加密）',
    `cooperation_level`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '合作等级：0=潜在，1=合作中，2=核心，3=已流失',
    `status`              TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '审核状态：0=待审核，1=已通过，2=已拒绝',
    `last_recruit_time`   DATETIME     DEFAULT NULL COMMENT '最近一次招聘时间',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`             INT(1)       DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业表';

-- ====================================
-- 3. 岗位表
-- ====================================
DROP TABLE IF EXISTS `job`;
CREATE TABLE `job` (
    `id`               BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
    `company_id`       BIGINT(20)   NOT NULL COMMENT '所属企业ID',
    `title`            VARCHAR(100) NOT NULL COMMENT '岗位名称',
    `salary_range`     VARCHAR(50)  DEFAULT NULL COMMENT '薪资范围（如"8k-12k"）',
    `education`        VARCHAR(20)  DEFAULT NULL COMMENT '学历要求',
    `location`         VARCHAR(100) DEFAULT NULL COMMENT '工作地点',
    `description`      TEXT         DEFAULT NULL COMMENT '岗位描述',
    `requirement`      TEXT         DEFAULT NULL COMMENT '任职要求',
    `deadline`         DATE         DEFAULT NULL COMMENT '截止日期',
    `status`           TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '状态：0=草稿，1=已发布，2=已关闭，3=暂停',
    `created_by`       BIGINT(20)   NOT NULL COMMENT '发布者ID（教师或HR）',
    `view_count`       INT(11)      NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `qr_code_url`      VARCHAR(255) DEFAULT NULL COMMENT '专属二维码图片路径',
    `trace_id`         VARCHAR(32)  NOT NULL COMMENT '唯一追踪ID',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `ai_keywords`      VARCHAR(500) DEFAULT NULL COMMENT '从岗位描述中提取的关键词（JSON）',
    `required_skills`  VARCHAR(500) DEFAULT NULL COMMENT '所需技能标签（JSON）',
    `deleted`          INT(1)       DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trace_id` (`trace_id`),
    KEY `idx_company_id` (`company_id`),
    KEY `idx_status_deadline` (`status`, `deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位表';

-- ====================================
-- 4. 简历表
-- ====================================
DROP TABLE IF EXISTS `resume`;
CREATE TABLE `resume` (
    `id`              BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '简历ID',
    `student_id`      BIGINT(20)   NOT NULL COMMENT '学生ID',
    `education`       TEXT         DEFAULT NULL COMMENT '教育经历（JSON）',
    `internship`      TEXT         DEFAULT NULL COMMENT '实习经历（JSON）',
    `skills`          VARCHAR(500) DEFAULT NULL COMMENT '技能证书',
    `self_evaluation` TEXT         DEFAULT NULL COMMENT '自我评价',
    `pdf_url`         VARCHAR(255) DEFAULT NULL COMMENT '上传的PDF简历路径',
    `is_default`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否为当前默认简历：0=否，1=是',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `skill_tags`      VARCHAR(500) DEFAULT NULL COMMENT '从简历中提取的技能标签（JSON）',
    `job_target`      VARCHAR(500) DEFAULT NULL COMMENT '求职意向',
    `deleted`         INT(1)       DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生简历表';

-- ====================================
-- 5. 投递记录表
-- ====================================
DROP TABLE IF EXISTS `delivery`;
CREATE TABLE `delivery` (
    `id`                 BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '投递ID',
    `student_id`         BIGINT(20)   NOT NULL COMMENT '学生ID',
    `job_id`             BIGINT(20)   NOT NULL COMMENT '岗位ID',
    `resume_version`     VARCHAR(50)  NOT NULL COMMENT '投递时的简历版本快照',
    `status`             TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '状态：0=已投递，1=企业已查看，2=待面试，3=已录用，4=不合适',
    `interview_time`     DATETIME     DEFAULT NULL COMMENT '面试时间',
    `interview_location` VARCHAR(200) DEFAULT NULL COMMENT '面试地点',
    `feedback`           VARCHAR(500) DEFAULT NULL COMMENT '企业反馈',
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投递时间',
    `deleted`            INT(1)       DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投递记录表';

-- ====================================
-- 6. 班级表
-- ====================================
DROP TABLE IF EXISTS `class`;
CREATE TABLE `class` (
    `id`          BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '班级ID',
    `name`        VARCHAR(50) NOT NULL COMMENT '班级名称',
    `teacher_id`  BIGINT(20)  NOT NULL COMMENT '班主任/教师ID',
    `major`       VARCHAR(50) DEFAULT NULL COMMENT '专业名称',
    `grade`       VARCHAR(10) DEFAULT NULL COMMENT '年级（如2023级）',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`     INT(1)      DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    KEY `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级表';

-- ====================================
-- 7. 学生-班级关联表
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
-- 8. 岗位变更申请记录表
-- ====================================
DROP TABLE IF EXISTS `job_change_apply`;
CREATE TABLE `job_change_apply` (
    `id`                BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    `job_id`            BIGINT(20) NOT NULL COMMENT '岗位ID',
    `hr_id`             BIGINT(20) NOT NULL COMMENT '申请HR ID',
    `change_content`    TEXT       NOT NULL COMMENT '变更内容（JSON）',
    `status`            TINYINT(1) NOT NULL DEFAULT 0 COMMENT '状态：0=待审核，1=通过，2=拒绝',
    `review_teacher_id` BIGINT(20) DEFAULT NULL COMMENT '审核教师ID',
    `create_time`       DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_hr_id` (`hr_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业-岗位变更申请记录表';

-- ====================================
-- 9. 操作日志表
-- ====================================
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
    `id`             BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id`        BIGINT(20)   NOT NULL COMMENT '操作用户ID',
    `operation_type` VARCHAR(50)  NOT NULL COMMENT '操作类型（查看简历、下载等）',
    `target_id`      VARCHAR(100) DEFAULT NULL COMMENT '目标对象ID（如简历ID）',
    `ip_address`     VARCHAR(50)  DEFAULT NULL COMMENT '操作IP',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id_create_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ====================================
-- 10. AI解析日志表
-- ====================================
DROP TABLE IF EXISTS `ai_parse_log`;
CREATE TABLE `ai_parse_log` (
    `id`                  BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `teacher_id`          BIGINT(20)   NOT NULL COMMENT '发起解析的教师ID',
    `raw_message`         TEXT         NOT NULL COMMENT '原始转发消息（文本或图片描述）',
    `parsed_result`       TEXT         NOT NULL COMMENT 'AI解析输出的结构化JSON',
    `is_manual_corrected` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否人工修正：0=未修正，1=已人工修正',
    `corrected_result`    TEXT         DEFAULT NULL COMMENT '人工修正后的JSON',
    `confidence_score`    DECIMAL(5,2) DEFAULT NULL COMMENT 'AI整体置信度（0-1）',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '解析时间',
    PRIMARY KEY (`id`),
    KEY `idx_teacher_id_create_time` (`teacher_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI解析日志表';

-- ====================================
-- 11. 人岗匹配记录表
-- ====================================
DROP TABLE IF EXISTS `job_match_record`;
CREATE TABLE `job_match_record` (
    `id`           BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `job_id`       BIGINT(20)   NOT NULL COMMENT '岗位ID',
    `student_id`   BIGINT(20)   NOT NULL COMMENT '学生ID',
    `match_score`  DECIMAL(5,2) NOT NULL COMMENT '匹配度分数（0-1）',
    `match_reason` VARCHAR(255) DEFAULT NULL COMMENT '匹配理由（如"技能匹配：Java,Spring"）',
    `is_pushed`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否推送：0=未推送，1=已推送',
    `push_time`    DATETIME     DEFAULT NULL COMMENT '推送时间',
    `is_clicked`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否点击：0=未点击，1=已点击',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '计算时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id_match_score` (`job_id`, `match_score`),
    KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人岗匹配记录表';

-- ====================================
-- 12. 简历智能评分记录表
-- ====================================
DROP TABLE IF EXISTS `resume_score_log`;
CREATE TABLE `resume_score_log` (
    `id`           BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `job_id`       BIGINT(20) NOT NULL COMMENT '岗位ID',
    `delivery_id`  BIGINT(20) NOT NULL COMMENT '投递记录ID',
    `score`        INT(11)    NOT NULL COMMENT '总分（如0-100）',
    `score_detail` TEXT       DEFAULT NULL COMMENT '评分细则JSON（如"技能得分90，经验得分70"）',
    `create_time`  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id_score` (`job_id`, `score`),
    KEY `idx_delivery_id` (`delivery_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历智能评分记录表';

-- ====================================
-- 13. AI反馈日志表
-- ====================================
DROP TABLE IF EXISTS `ai_feedback_log`;
CREATE TABLE `ai_feedback_log` (
    `id`              BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id`         BIGINT(20)   NOT NULL COMMENT '用户ID',
    `feedback_type`   VARCHAR(50)  NOT NULL COMMENT '反馈类型：parse_error / match_bad / match_good',
    `target_id`       BIGINT(20)   NOT NULL COMMENT '目标对象ID（如简历ID、岗位ID）',
    `feedback_content` VARCHAR(500) DEFAULT NULL COMMENT '反馈内容',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_target_id` (`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI反馈日志表';

-- ====================================
-- 14. 收藏表（小程序补充）
-- ====================================
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
    `id`          BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    `student_id`  BIGINT(20) NOT NULL COMMENT '学生ID',
    `job_id`      BIGINT(20) NOT NULL COMMENT '岗位ID',
    `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    `deleted`     INT(1)     DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_job` (`student_id`, `job_id`),
    KEY `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- ====================================
-- 15. 消息表（小程序补充）
-- ====================================
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
    `id`          BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `student_id`  BIGINT(20) NOT NULL COMMENT '学生ID',
    `title`       VARCHAR(100) DEFAULT NULL COMMENT '消息标题',
    `content`     TEXT         DEFAULT NULL COMMENT '消息内容',
    `type`        TINYINT(1)   DEFAULT 0 COMMENT '消息类型：0=系统通知，1=面试邀请，2=投递反馈',
    `is_read`     TINYINT(1)   DEFAULT 0 COMMENT '是否已读：0=未读，1=已读',
    `related_id`  BIGINT(20)   DEFAULT NULL COMMENT '关联对象ID（如投递ID）',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     INT(1)       DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    KEY `idx_student_read` (`student_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ====================================
-- 测试数据
-- ====================================

-- 用户
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `phone`, `status`, `company_id`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 3, NULL, 1, NULL),
('T001',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '张老师', 1, '13800000001', 1, NULL),
('T002',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '李老师', 1, '13800000002', 1, NULL),
('HR001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '王经理', 2, '13900000001', 1, 1),
('HR002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '赵主管', 2, '13900000002', 1, 2),
('S001',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '小明', 0, '13600000001', 1, NULL),
('S002',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '小红', 0, '13600000002', 1, NULL),
('S003',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '小华', 0, '13600000003', 1, NULL),
('S004',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '小丽', 0, '13600000004', 1, NULL),
('S005',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '小强', 0, '13600000005', 1, NULL);

-- 企业
INSERT INTO `company` (`name`, `short_name`, `industry`, `address`, `contact_person`, `contact_phone`, `cooperation_level`, `status`) VALUES
('腾讯科技有限公司', '腾讯', '互联网', '深圳市南山区', '马总', NULL, 2, 1),
('阿里巴巴集团', '阿里', '互联网', '杭州市余杭区', '张总', NULL, 2, 1),
('华为技术有限公司', '华为', '通信/硬件', '深圳市龙岗区', '任正非', NULL, 1, 1),
('字节跳动科技有限公司', '字节', '互联网', '北京市海淀区', '张一鸣', NULL, 1, 1),
('比亚迪股份有限公司', '比亚迪', '汽车/新能源', '深圳市坪山区', '王传福', NULL, 0, 0);

-- 岗位
INSERT INTO `job` (`company_id`, `title`, `salary_range`, `education`, `location`, `description`, `requirement`, `status`, `created_by`, `trace_id`, `ai_keywords`, `required_skills`) VALUES
(1, 'Java开发工程师', '10k-20k', '本科', '深圳', '负责后端服务开发与维护', '熟练掌握Java、Spring Boot、MySQL', 1, 1, 'trace-java-001', '["Java", "Spring", "MySQL"]', '["Java", "Spring Boot", "MySQL", "Redis"]'),
(1, '前端开发工程师', '10k-18k', '本科', '深圳', '负责Web前端开发', '熟练掌握Vue.js、React、TypeScript', 1, 1, 'trace-fe-002', '["Vue", "React", "TypeScript"]', '["Vue.js", "React", "TypeScript", "Webpack"]'),
(2, 'Python开发工程师', '12k-22k', '本科', '杭州', '负责数据平台开发', '熟练掌握Python、Django、数据处理', 1, 1, 'trace-py-003', '["Python", "Django", "Data"]', '["Python", "Django", "Pandas", "NumPy"]'),
(3, '嵌入式软件工程师', '15k-25k', '硕士', '深圳', '负责嵌入式系统开发', '熟练掌握C/C++、嵌入式Linux', 1, 2, 'trace-embed-004', '["C", "C++", "Embedded"]', '["C", "C++", "Linux", "ARM"]'),
(4, '算法工程师', '20k-35k', '硕士', '北京', '负责推荐算法开发', '熟练掌握机器学习、深度学习', 1, 2, 'trace-algo-005', '["ML", "DL", "Algorithm"]', '["Python", "TensorFlow", "PyTorch", "ML"]'),
(5, '硬件测试工程师', '8k-15k', '本科', '深圳', '负责硬件产品测试', '熟悉硬件测试流程、测试工具', 1, 1, 'trace-hw-006', '["Test", "Hardware"]', '["硬件测试", "示波器", "万用表"]');

-- 班级
INSERT INTO `class` (`name`, `teacher_id`, `major`, `grade`) VALUES
('计算机科学与技术2023级1班', 1, '计算机科学与技术', '2023级'),
('软件工程2023级1班', 1, '软件工程', '2023级'),
('电子信息工程2023级1班', 2, '电子信息工程', '2023级');

-- 学生-班级关联
INSERT INTO `student_class` (`student_id`, `class_id`) VALUES
(6, 1), (7, 1), (8, 2), (9, 2), (10, 3);

-- 简历
INSERT INTO `resume` (`student_id`, `education`, `internship`, `skills`, `self_evaluation`, `job_target`) VALUES
(6,
 '[{"school":"XX职业学院","major":"计算机科学与技术","degree":"本科","start":"2020-09","end":"2024-06"}]',
 '[{"company":"腾讯","position":"Java开发实习生","duration":"2023-07至2023-12"}]',
 'Java, Spring Boot, MySQL, Redis',
 '热爱编程，学习能力强，有良好的团队协作能力',
 'Java开发工程师'),
(7,
 '[{"school":"XX职业学院","major":"计算机科学与技术","degree":"本科","start":"2020-09","end":"2024-06"}]',
 '[{"company":"阿里","position":"前端开发实习生","duration":"2023-06至2023-12"}]',
 'Vue.js, React, TypeScript, Webpack',
 '对前端技术充满热情，善于用户体验优化',
 '前端开发工程师'),
(8,
 '[{"school":"XX职业学院","major":"软件工程","degree":"本科","start":"2020-09","end":"2024-06"}]',
 '[{"company":"字节","position":"Python开发实习生","duration":"2023-07至2023-12"}]',
 'Python, Django, Pandas, NumPy',
 '对数据分析和机器学习有浓厚兴趣',
 'Python开发工程师'),
(9,
 '[{"school":"XX职业学院","major":"软件工程","degree":"本科","start":"2020-09","end":"2024-06"}]',
 NULL,
 'C, C++, Linux, ARM',
 '嵌入式系统爱好者，动手能力强',
 '嵌入式软件工程师'),
(10,
 '[{"school":"XX职业学院","major":"电子信息工程","degree":"本科","start":"2020-09","end":"2024-06"}]',
 '[{"company":"华为","position":"硬件测试实习生","duration":"2023-06至2023-12"}]',
 '硬件测试, 示波器, 万用表',
 '细心严谨，对硬件测试有独到见解',
 '硬件测试工程师');

-- 投递记录
INSERT INTO `delivery` (`student_id`, `job_id`, `resume_version`, `status`) VALUES
(6, 1, 'v1.0', 2),  -- 小明 -> Java工程师（待面试）
(6, 2, 'v1.0', 1),  -- 小明 -> 前端工程师（已查看）
(7, 2, 'v1.0', 3),  -- 小红 -> 前端工程师（已录用）
(8, 3, 'v1.0', 0),  -- 小华 -> Python工程师（已投递）
(9, 4, 'v1.0', 1),  -- 小丽 -> 嵌入式工程师（已查看）
(10, 6, 'v1.0', 2); -- 小强 -> 硬件测试工程师（待面试）

-- 人岗匹配
INSERT INTO `job_match_record` (`job_id`, `student_id`, `match_score`, `match_reason`, `is_pushed`) VALUES
(1, 6, 0.92, '技能匹配：Java, Spring Boot, MySQL', 1),
(2, 7, 0.88, '技能匹配：Vue.js, React, TypeScript', 1),
(3, 8, 0.85, '技能匹配：Python, Django', 1),
(4, 9, 0.90, '技能匹配：C, C++, Linux', 0),
(5, 10, 0.78, '技能匹配：硬件测试', 1);

-- 简历评分
INSERT INTO `resume_score_log` (`job_id`, `delivery_id`, `score`, `score_detail`) VALUES
(1, 1, 88, '{"技能得分":90,"项目经验":85,"教育背景":88,"自我评价":85}'),
(2, 2, 85, '{"技能得分":88,"项目经验":82,"教育背景":85,"自我评价":85}'),
(3, 4, 82, '{"技能得分":85,"项目经验":80,"教育背景":82,"自我评价":80}');

-- 收藏
INSERT INTO `favorite` (`student_id`, `job_id`) VALUES
(6, 1), (6, 3);

-- 消息
INSERT INTO `message` (`student_id`, `title`, `content`, `type`, `is_read`) VALUES
(6, '投递成功通知', '您的简历已成功投递，请等待企业反馈', 0, 1),
(6, '简历被查看', '您的简历已被企业查看，祝您好运！', 0, 0),
(6, '面试邀请', '您投递的Java开发工程师岗位已通过初筛，请等待面试安排', 1, 0),
(6, '系统通知', 'AI简历评分功能已上线，欢迎体验', 2, 1);

-- 操作日志
INSERT INTO `operation_log` (`user_id`, `operation_type`, `target_id`, `ip_address`) VALUES
(1, '查看简历', '6', '127.0.0.1'),
(1, '下载简历', '7', '127.0.0.1'),
(2, '发布岗位', '1', '127.0.0.1'),
(3, '查看投递', '1', '127.0.0.1');

SELECT CONCAT('数据库初始化完成！共创建15张表，已插入测试数据。') AS message;
