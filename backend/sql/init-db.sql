-- ================================================
-- 职业院校校企招聘与就业管理平台
-- 完整数据库初始化脚本
-- 用法：mysql -u root -p < init-db.sql
-- ================================================

CREATE DATABASE IF NOT EXISTS `campus_recruitment`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `campus_recruitment`;

-- ================================================
-- 1. 用户表
-- ================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`            BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`      VARCHAR(50)  NOT NULL COMMENT '用户名（学号/工号）',
    `password`      VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `real_name`     VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    `role`          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '角色：0=学生，1=教师，2=企业HR，3=管理员',
    `phone`         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `email`         VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `gender`        TINYINT(1)   DEFAULT 0 COMMENT '性别：0=未知，1=男，2=女',
    `wechat_openid` VARCHAR(100) DEFAULT NULL COMMENT '微信OpenID',
    `avatar_url`    VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `company_id`    BIGINT(20)   DEFAULT NULL COMMENT '关联企业ID（仅HR角色使用）',
    `status`        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '状态：0=禁用，1=正常',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       INT(1)       DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ================================================
-- 2. 企业表
-- ================================================
DROP TABLE IF EXISTS `company`;
CREATE TABLE `company` (
    `id`                  BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '企业ID',
    `name`                VARCHAR(100) NOT NULL COMMENT '企业全称',
    `short_name`          VARCHAR(50)  DEFAULT NULL COMMENT '简称',
    `industry`            VARCHAR(50)  DEFAULT NULL COMMENT '行业',
    `address`             VARCHAR(200) DEFAULT NULL COMMENT '地址',
    `contact_person`      VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
    `contact_phone`       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    `cooperation_level`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '合作等级：0=潜在，1=合作中，2=核心',
    `status`              TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '审核状态：0=待审核，1=已通过，2=已拒绝',
    `last_recruit_time`   DATETIME     DEFAULT NULL COMMENT '最近一次招聘时间',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`             INT(1)       DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业表';

-- ================================================
-- 3. 岗位表
-- ================================================
DROP TABLE IF EXISTS `job`;
CREATE TABLE `job` (
    `id`               BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
    `company_id`       BIGINT(20)   NOT NULL COMMENT '所属企业ID',
    `title`            VARCHAR(100) NOT NULL COMMENT '岗位名称',
    `salary_range`     VARCHAR(50)  DEFAULT NULL COMMENT '薪资范围',
    `education`        VARCHAR(20)  DEFAULT NULL COMMENT '学历要求',
    `location`         VARCHAR(100) DEFAULT NULL COMMENT '工作地点',
    `description`      TEXT         DEFAULT NULL COMMENT '岗位描述（详细）',
    `requirement`      TEXT         DEFAULT NULL COMMENT '任职要求（详细）',
    `deadline`         DATE         DEFAULT NULL COMMENT '截止日期',
    `status`           TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '状态：0=草稿，1=已发布，2=已关闭',
    `created_by`       BIGINT(20)   NOT NULL COMMENT '发布者ID',
    `view_count`       INT(11)      NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `trace_id`         VARCHAR(32)  NOT NULL COMMENT '唯一追踪ID',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `required_skills`  VARCHAR(500) DEFAULT NULL COMMENT '所需技能标签（JSON）',
    `deleted`          INT(1)       DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trace_id` (`trace_id`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位表';

-- ================================================
-- 4. 简历表
-- ================================================
DROP TABLE IF EXISTS `resume`;
CREATE TABLE `resume` (
    `id`              BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '简历ID',
    `student_id`      BIGINT(20)   NOT NULL COMMENT '学生ID',
    `education`       TEXT         DEFAULT NULL COMMENT '教育经历（JSON数组）',
    `internship`      TEXT         DEFAULT NULL COMMENT '实习经历（JSON数组）',
    `skills`          VARCHAR(500) DEFAULT NULL COMMENT '技能证书',
    `self_evaluation` TEXT         DEFAULT NULL COMMENT '自我评价',
    `is_default`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否默认简历',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `job_target`      VARCHAR(500) DEFAULT NULL COMMENT '求职意向',
    `ai_analysis`     TEXT         DEFAULT NULL COMMENT 'AI简历分析结果（JSON）',
    `deleted`         INT(1)       DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生简历表';

-- ================================================
-- 5. 投递记录表
-- ================================================
DROP TABLE IF EXISTS `delivery`;
CREATE TABLE `delivery` (
    `id`                 BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '投递ID',
    `student_id`         BIGINT(20)   NOT NULL COMMENT '学生ID',
    `job_id`             BIGINT(20)   NOT NULL COMMENT '岗位ID',
    `resume_version`     VARCHAR(50)  NOT NULL DEFAULT 'v1.0' COMMENT '简历版本',
    `status`             TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '状态：0=待查看，1=已查看，2=面试中，3=已录用，4=不合适',
    `interview_time`     DATETIME     DEFAULT NULL COMMENT '面试时间',
    `interview_location` VARCHAR(200) DEFAULT NULL COMMENT '面试地点',
    `feedback`           VARCHAR(500) DEFAULT NULL COMMENT '企业反馈',
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投递时间',
    `deleted`            INT(1)       DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投递记录表';

-- ================================================
-- 6. 班级表
-- ================================================
DROP TABLE IF EXISTS `class`;
CREATE TABLE `class` (
    `id`          BIGINT(20)  NOT NULL AUTO_INCREMENT COMMENT '班级ID',
    `name`        VARCHAR(50) NOT NULL COMMENT '班级名称',
    `teacher_id`  BIGINT(20)  NOT NULL COMMENT '班主任/教师ID',
    `major`       VARCHAR(50) DEFAULT NULL COMMENT '专业名称',
    `grade`       VARCHAR(10) DEFAULT NULL COMMENT '年级',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`     INT(1)      DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    KEY `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级表';

-- ================================================
-- 7. 学生-班级关联表
-- ================================================
DROP TABLE IF EXISTS `student_class`;
CREATE TABLE `student_class` (
    `id`         BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id` BIGINT(20) NOT NULL COMMENT '学生ID',
    `class_id`   BIGINT(20) NOT NULL COMMENT '班级ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_class` (`student_id`, `class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生-班级关联表';

-- ================================================
-- 8. 浏览记录表
-- ================================================
DROP TABLE IF EXISTS `browse_history`;
CREATE TABLE `browse_history` (
    `id`          BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `student_id`  BIGINT(20) NOT NULL COMMENT '学生ID',
    `job_id`      BIGINT(20) NOT NULL COMMENT '岗位ID',
    `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_job` (`student_id`, `job_id`),
    KEY `idx_student_time` (`student_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浏览记录表';

-- ================================================
-- 9. 岗位变更申请记录表
-- ================================================
DROP TABLE IF EXISTS `job_change_apply`;
CREATE TABLE `job_change_apply` (
    `id`                BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    `job_id`            BIGINT(20) NOT NULL COMMENT '岗位ID',
    `hr_id`             BIGINT(20) NOT NULL COMMENT '申请HR ID',
    `change_content`    TEXT       NOT NULL COMMENT '变更内容（JSON）',
    `status`            TINYINT(1) NOT NULL DEFAULT 0 COMMENT '状态：0=待审核，1=通过，2=拒绝',
    `review_teacher_id` BIGINT(20) DEFAULT NULL COMMENT '审核教师ID',
    `create_time`       DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位变更申请记录表';

-- ================================================
-- 10. 操作日志表
-- ================================================
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
    `id`             BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id`        BIGINT(20)   NOT NULL COMMENT '操作用户ID',
    `operation_type` VARCHAR(50)  NOT NULL COMMENT '操作类型',
    `target_id`      VARCHAR(100) DEFAULT NULL COMMENT '目标对象ID',
    `ip_address`     VARCHAR(50)  DEFAULT NULL COMMENT '操作IP',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ================================================
-- 11. AI解析日志表
-- ================================================
DROP TABLE IF EXISTS `ai_parse_log`;
CREATE TABLE `ai_parse_log` (
    `id`                  BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `teacher_id`          BIGINT(20)   NOT NULL COMMENT '发起解析的教师ID',
    `raw_message`         TEXT         NOT NULL COMMENT '原始文本',
    `parsed_result`       TEXT         NOT NULL COMMENT 'AI解析结果（JSON）',
    `is_manual_corrected` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否人工修正',
    `corrected_result`    TEXT         DEFAULT NULL COMMENT '修正后的JSON',
    `confidence_score`    DECIMAL(5,2) DEFAULT NULL COMMENT 'AI置信度（0-1）',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '解析时间',
    PRIMARY KEY (`id`),
    KEY `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI解析日志表';

-- ================================================
-- 12. 人岗匹配记录表
-- ================================================
DROP TABLE IF EXISTS `job_match_record`;
CREATE TABLE `job_match_record` (
    `id`           BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `job_id`       BIGINT(20)   NOT NULL COMMENT '岗位ID',
    `student_id`   BIGINT(20)   NOT NULL COMMENT '学生ID',
    `match_score`  DECIMAL(5,2) NOT NULL COMMENT '匹配度（0-1）',
    `match_reason` VARCHAR(255) DEFAULT NULL COMMENT '匹配理由',
    `is_pushed`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已推送',
    `push_time`    DATETIME     DEFAULT NULL COMMENT '推送时间',
    `is_clicked`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已点击',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '计算时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人岗匹配记录表';

-- ================================================
-- 13. 简历评分记录表
-- ================================================
DROP TABLE IF EXISTS `resume_score_log`;
CREATE TABLE `resume_score_log` (
    `id`           BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `job_id`       BIGINT(20) NOT NULL COMMENT '岗位ID',
    `delivery_id`  BIGINT(20) NOT NULL COMMENT '投递记录ID',
    `score`        INT(11)    NOT NULL COMMENT '总分',
    `score_detail` TEXT       DEFAULT NULL COMMENT '评分细则（JSON）',
    `create_time`  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_delivery_id` (`delivery_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历评分记录表';

-- ================================================
-- 14. AI反馈日志表
-- ================================================
DROP TABLE IF EXISTS `ai_feedback_log`;
CREATE TABLE `ai_feedback_log` (
    `id`               BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id`          BIGINT(20)   NOT NULL COMMENT '用户ID',
    `feedback_type`    VARCHAR(50)  NOT NULL COMMENT '反馈类型',
    `target_id`        BIGINT(20)   NOT NULL COMMENT '目标对象ID',
    `feedback_content` VARCHAR(500) DEFAULT NULL COMMENT '反馈内容',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI反馈日志表';

-- ================================================
-- 15. 收藏表
-- ================================================
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
    `id`          BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    `student_id`  BIGINT(20) NOT NULL COMMENT '学生ID',
    `job_id`      BIGINT(20) NOT NULL COMMENT '岗位ID',
    `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    `deleted`     INT(1)     DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_job` (`student_id`, `job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- ================================================
-- 16. 消息表
-- ================================================
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
    `id`          BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `student_id`  BIGINT(20) NOT NULL COMMENT '学生ID',
    `title`       VARCHAR(100) DEFAULT NULL COMMENT '消息标题',
    `content`     TEXT         DEFAULT NULL COMMENT '消息内容',
    `type`        TINYINT(1)   DEFAULT 0 COMMENT '类型：0=系统，1=面试，2=投递',
    `is_read`     TINYINT(1)   DEFAULT 0 COMMENT '是否已读',
    `related_id`  BIGINT(20)   DEFAULT NULL COMMENT '关联对象ID',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`     INT(1)       DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ================================================
-- 填充真实感测试数据
-- ================================================

-- ── 用户（密码统一为 "123456" 的 BCrypt 加密值） ──
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `phone`, `email`, `gender`, `status`, `company_id`) VALUES
-- 管理员
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 3, NULL, NULL, 0, 1, NULL),

-- 教师
('T001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '张建国', 1, '13800138001', 'zhangjg@school.edu.cn', 1, 1, NULL),
('T002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '李秀华', 1, '13800138002', 'lixh@school.edu.cn', 2, 1, NULL),
('T003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '陈伟强', 1, '13800138003', 'chenwq@school.edu.cn', 1, 1, NULL),

-- HR
('HR001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '王丽', 2, '13900139001', 'wangli@chuangxiang.com', 2, 1, 1),
('HR002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '赵明', 2, '13900139002', 'zhaoming@lanqiao.com', 1, 1, 2),

-- 学生（10人）
('S001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '张明', 0, '13600136001', 'zhangming@stu.edu.cn', 1, 1, NULL),
('S002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '李婷', 0, '13600136002', 'liting@stu.edu.cn', 2, 1, NULL),
('S003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '王浩', 0, '13600136003', 'wanghao@stu.edu.cn', 1, 1, NULL),
('S004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '陈雪', 0, '13600136004', 'chenxue@stu.edu.cn', 2, 1, NULL),
('S005', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '刘强', 0, '13600136005', 'liuqiang@stu.edu.cn', 1, 1, NULL),
('S006', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '黄丽', 0, '13600136006', 'huangli@stu.edu.cn', 2, 1, NULL),
('S007', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '周杰', 0, '13600136007', 'zhoujie@stu.edu.cn', 1, 1, NULL),
('S008', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '吴芳', 0, '13600136008', 'wufang@stu.edu.cn', 2, 1, NULL),
('S009', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '郑鹏', 0, '13600136009', 'zhengpeng@stu.edu.cn', 1, 1, NULL),
('S010', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '孙雅', 0, '13600136010', 'sunya@stu.edu.cn', 2, 1, NULL);

-- ── 企业 ──
INSERT INTO `company` (`name`, `short_name`, `industry`, `address`, `contact_person`, `contact_phone`, `cooperation_level`, `status`) VALUES
('广州创想科技有限公司', '创想科技', '互联网/软件', '广州市天河区软件路15号', '王丽', '13900139001', 2, 1),
('深圳蓝桥信息技术有限公司', '蓝桥信息', '互联网/软件', '深圳市南山区科技园南路8号', '赵明', '13900139002', 2, 1),
('珠海鼎力软件开发有限公司', '鼎力软件', '软件/信息技术', '珠海市香洲区港湾大道168号', '林总', '13800000010', 1, 1),
('佛山智造科技有限公司', '智造科技', '智能制造/工业互联网', '佛山市南海区狮山镇科技路22号', '何经理', '13800000011', 1, 1),
('东莞华强电子有限公司', '华强电子', '电子/半导体', '东莞市长安镇振安东路88号', '刘总', '13800000012', 0, 0);

-- ── 岗位（每条带详细的职位描述和要求 — 让 AI 评分有据可依）──
INSERT INTO `job` (`company_id`, `title`, `salary_range`, `education`, `location`, `description`, `requirement`, `status`, `created_by`, `trace_id`, `required_skills`) VALUES
(1, 'Java后端开发工程师', '8K-15K', '大专及以上', '广州',
 '岗位职责：\n1. 参与公司核心业务系统的后端开发，使用 Java + Spring Boot 框架\n2. 负责 RESTful API 的设计、开发与文档编写\n3. 与前端工程师协作完成前后端联调\n4. 参与数据库表设计及 SQL 优化\n5. 参与代码 Review，保证代码质量',
 '任职要求：\n1. 计算机相关专业，大专及以上学历\n2. 熟悉 Java 基础，掌握 Spring Boot、MyBatis 框架\n3. 熟悉 MySQL 数据库，能写复杂 SQL\n4. 了解 Redis、RabbitMQ 等中间件\n5. 了解 Git 版本控制\n6. 有良好的团队协作和沟通能力',
 1, 1, 'trace-java-001', '["Java","Spring Boot","MyBatis","MySQL","Redis"]'),

(1, '前端开发工程师', '8K-14K', '大专及以上', '广州',
 '岗位职责：\n1. 负责公司管理后台和移动端 H5 页面的前端开发\n2. 使用 Vue.js 框架进行组件化开发\n3. 与 UI 设计师协作实现设计稿\n4. 参与前端性能优化和 bug 修复\n5. 编写可复用的组件和工具函数',
 '任职要求：\n1. 计算机相关专业，大专及以上学历\n2. 熟练掌握 HTML5、CSS3、JavaScript 基础\n3. 熟练使用 Vue.js 框架，了解其生态（Vue Router、Pinia）\n4. 了解前端工程化（Webpack/Vite）\n5. 了解 TypeScript 优先\n6. 有小程序开发经验者优先',
 1, 1, 'trace-fe-002', '["Vue.js","JavaScript","TypeScript","HTML5","CSS3"]'),

(2, 'Python数据分析师', '10K-18K', '本科及以上', '深圳',
 '岗位职责：\n1. 负责业务数据的采集、清洗和分析\n2. 使用 Python 开发数据处理脚本和自动化报表\n3. 使用 SQL 从数据库提取业务数据\n4. 制作数据可视化图表和数据分析报告\n5. 与运营团队协作，提供数据驱动的决策建议',
 '任职要求：\n1. 计算机、统计、数学等相关专业本科及以上学历\n2. 熟练掌握 Python 语言，了解 Pandas、NumPy 等数据分析库\n3. 熟练使用 SQL 进行数据查询\n4. 了解数据可视化工具（Matplotlib、ECharts）\n5. 对数据分析有浓厚兴趣\n6. 了解机器学习基础者优先',
 1, 1, 'trace-py-003', '["Python","Pandas","NumPy","SQL","数据分析"]'),

(2, '软件测试工程师', '8K-13K', '大专及以上', '深圳',
 '岗位职责：\n1. 参与产品需求评审，编写测试用例\n2. 执行功能测试、回归测试、兼容性测试\n3. 提交和跟踪 bug，推动问题解决\n4. 使用工具进行接口测试和性能测试\n5. 编写测试报告，评估产品质量',
 '任职要求：\n1. 计算机相关专业，大专及以上学历\n2. 了解软件测试理论和方法\n3. 熟悉至少一种测试工具（Postman/JMeter）\n4. 了解数据库基本操作，能写简单 SQL\n5. 工作细心、有耐心，善于发现和分析问题',
 1, 2, 'trace-test-004', '["测试","Postman","JMeter","SQL"]'),

(3, '嵌入式软件工程师', '12K-20K', '本科及以上', '珠海',
 '岗位职责：\n1. 负责嵌入式产品的软件开发，使用 C/C++ 语言\n2. 编写裸机驱动程序和嵌入式 Linux 应用\n3. 参与硬件原理图评审，编写硬件测试程序\n4. 负责产品功能调试和性能优化\n5. 编写技术文档和设计说明',
 '任职要求：\n1. 电子信息、自动化、计算机等相关专业本科及以上学历\n2. 熟练掌握 C/C++ 编程语言\n3. 了解嵌入式 Linux 开发环境，了解交叉编译\n4. 了解常见通信协议（UART/I2C/SPI）\n5. 有单片机或 ARM 开发经验者优先\n6. 了解 RTOS 概念者优先',
 1, 2, 'trace-embed-005', '["C","C++","Linux","ARM","嵌入式"]'),

(3, '产品助理（实习）', '3K-5K', '大专及以上', '珠海',
 '岗位职责：\n1. 协助产品经理完成需求调研和竞品分析\n2. 编写产品需求文档和原型图\n3. 跟进产品开发进度，协调团队沟通\n4. 收集用户反馈，整理产品优化建议\n5. 参与产品测试和验收',
 '任职要求：\n1. 专业不限，大专及以上学历\n2. 对互联网产品有热情，喜欢体验各种 App\n3. 逻辑清晰，表达能力强\n4. 会使用 Axure/Figma/墨刀等原型工具优先\n5. 了解基础的 UI/UX 设计原则',
 1, 2, 'trace-pm-006', '["产品设计","Axure","需求分析","文档编写"]'),

(4, '新媒体运营专员', '6K-10K', '大专及以上', '佛山',
 '岗位职责：\n1. 负责公司公众号、抖音、小红书等新媒体平台的内容运营\n2. 策划和执行线上营销活动\n3. 撰写推文、制作短视频内容\n4. 分析运营数据，优化内容策略\n5. 维护粉丝社群，提升用户活跃度',
 '任职要求：\n1. 市场营销、传媒、中文等相关专业优先\n2. 熟悉主流新媒体平台规则和玩法\n3. 有良好的文字功底和内容创作能力\n4. 会使用 PS/剪映等基础设计剪辑工具\n5. 有运营个人账号经验者优先',
 1, 1, 'trace-media-007', '["新媒体","内容运营","文案","PS","视频剪辑"]'),

(5, '硬件测试技术员', '6K-10K', '大专及以上', '东莞',
 '岗位职责：\n1. 负责电子产品硬件功能测试和可靠性测试\n2. 根据测试方案搭建测试环境\n3. 记录测试数据，编写测试报告\n4. 使用示波器、万用表等仪器排查硬件问题\n5. 协助研发工程师进行问题复现和验证',
 '任职要求：\n1. 电子、通信、自动化等相关专业大专及以上学历\n2. 了解电子电路基础知识\n3. 会使用示波器、信号发生器、万用表等常见仪表\n4. 了解基本的焊接技能\n5. 工作认真负责，有质量意识',
 1, 1, 'trace-hw-008', '["硬件测试","示波器","万用表","电子电路"]');

-- ── 班级 ──
INSERT INTO `class` (`name`, `teacher_id`, `major`, `grade`) VALUES
('计算机应用技术2023级1班', 1, '计算机应用技术', '2023级'),
('计算机应用技术2024级1班', 1, '计算机应用技术', '2024级'),
('软件技术2023级1班', 2, '软件技术', '2023级'),
('电子商务2023级1班', 3, '电子商务', '2023级'),
('电子信息工程技术2023级1班', 3, '电子信息工程技术', '2023级');

-- ── 学生-班级关联 ──
INSERT INTO `student_class` (`student_id`, `class_id`) VALUES
(7, 1), (8, 1), (9, 2), (10, 3), (11, 3),
(12, 4), (13, 4), (14, 5), (15, 5), (16, 1);

-- ── 简历（每条包含完整的教育/技能/实习/自评 — AI 评分的核心数据源）──
INSERT INTO `resume` (`student_id`, `education`, `internship`, `skills`, `self_evaluation`, `job_target`) VALUES
-- S001 张明 — 目标 Java 开发
(7,
 '[{"school":"广州职业技术学院","major":"计算机应用技术","degree":"大专","startYear":"2023-09","endYear":"2026-06"}]',
 '[{"company":"广州创想科技有限公司","position":"Java开发实习生","start":"2025-07","end":"2025-12","description":"参与公司后台管理系统开发，使用Spring Boot+MyBatis实现用户管理、权限控制等功能模块"}]',
 'Java, Spring Boot, MyBatis, MySQL, Redis, Git, Linux基础',
 '热爱编程，有扎实的Java基础，能独立完成模块开发。在实习期间参与了2个真实项目上线。性格开朗，善于团队协作，乐于学习新技术。',
 'Java后端开发工程师'),

-- S002 李婷 — 目标 前端开发
(8,
 '[{"school":"广州职业技术学院","major":"计算机应用技术","degree":"大专","startYear":"2023-09","endYear":"2026-06"}]',
 '[{"company":"广州创想科技有限公司","position":"前端开发实习生","start":"2025-07","end":"2025-12","description":"使用Vue3 + Element Plus开发企业后台管理界面，负责数据可视化大屏的开发"}]',
 'Vue.js, JavaScript, TypeScript, HTML5, CSS3, ECharts, Git, 微信小程序',
 '对前端技术充满热情，喜欢将设计稿变成精致的页面。熟悉Vue全家桶开发模式，有小程序开发经验。善于与UI设计师沟通协作。',
 '前端开发工程师'),

-- S003 王浩 — 目标 Python/数据分析
(9,
 '[{"school":"广州职业技术学院","major":"软件技术","degree":"大专","startYear":"2023-09","endYear":"2026-06"}]',
 '[{"company":"广州数据科技有限公司","position":"数据分析实习生","start":"2025-07","end":"2025-09","description":"使用Python进行销售数据清洗和分析，制作日报周报数据看板"}]',
 'Python, Pandas, NumPy, SQL, Excel, ECharts, Flask',
 '数据敏感度高，逻辑思维强。在校期间参与了数据分析竞赛并获奖。熟练使用Python进行数据处理和可视化，了解基础的机器学习算法。',
 'Python数据分析师'),

-- S004 陈雪 — 目标 测试/产品
(10,
 '[{"school":"广州职业技术学院","major":"软件技术","degree":"大专","startYear":"2023-09","endYear":"2026-06"}]',
 NULL,
 'SQL, Postman, 基础Python, Excel, 文档编写, XMind',
 '细致认真，善于发现和总结问题。在校担任学生会干部，有良好的沟通和组织能力。了解软件测试流程，能编写规范的测试用例。',
 '软件测试工程师'),

-- S005 刘强 — 目标 嵌入式/硬件
(11,
 '[{"school":"深圳信息职业技术学院","major":"计算机应用技术","degree":"大专","startYear":"2023-09","endYear":"2026-06"}]',
 NULL,
 'C, C++, 基础Linux, 单片机（STM32）, 电路基础, 焊接',
 '动手能力强，喜欢钻研硬件和底层技术。在校期间参加电子设计大赛获得校级二等奖。了解嵌入式开发的基本流程。',
 '嵌入式软件工程师'),

-- S006 黄丽 — 目标 运营/产品
(12,
 '[{"school":"广州职业技术学院","major":"电子商务","degree":"大专","startYear":"2023-09","endYear":"2026-06"}]',
 '[{"company":"广州新媒体科技有限公司","position":"新媒体运营实习生","start":"2025-06","end":"2025-12","description":"负责公司小红书账号的内容策划和日常运营，3个月涨粉5000+，产出过10万+爆文"}]',
 '文案写作, PS, 剪映, 小红书/抖音运营, Excel, 基础数据分析',
 '创意丰富，网感好，熟悉主流新媒体平台的算法和玩法。实习期间独立运营账号并取得良好数据。善于用内容打动用户。',
 '新媒体运营'),

-- S007 周杰 — 目标 前端开发
(13,
 '[{"school":"广州职业技术学院","major":"电子商务","degree":"大专","startYear":"2023-09","endYear":"2026-06"}]',
 NULL,
 'HTML5, CSS3, JavaScript, Vue.js基础, Photoshop',
 '学习能力强，通过自学掌握了前端开发的基础技能。有自己的GitHub主页，提交过一些小项目。虽然没有实习经验，但有足够的学习热情和时间投入。',
 '前端开发工程师'),

-- S008 吴芳 — 目标 测试/技术支持
(14,
 '[{"school":"深圳信息职业技术学院","major":"电子信息工程技术","degree":"大专","startYear":"2023-09","endYear":"2026-06"}]',
 '[{"company":"深圳华强电子有限公司","position":"硬件测试实习生","start":"2025-07","end":"2025-10","description":"协助工程师进行电路板功能测试，使用示波器和万用表检测信号，记录100+条测试数据"}]',
 '电路分析, 示波器, 万用表, 信号发生器, 焊接, 基础C语言',
 '细心严谨，动手能力强。实习期间熟悉了电子产品测试的全流程。对硬件测试有浓厚兴趣，希望在这方面持续发展。',
 '硬件测试工程师'),

-- S009 郑鹏 — 目标 Java开发
(15,
 '[{"school":"深圳信息职业技术学院","major":"计算机应用技术","degree":"大专","startYear":"2023-09","endYear":"2026-06"}]',
 NULL,
 'Java基础, MySQL, HTML, CSS, JavaScript, Git',
 '在校期间认真学习编程知识，完成了Java Web课程设计项目。虽然暂无实习经验，但基础扎实，学习目标明确。有良好的编程习惯和代码规范意识。',
 'Java开发工程师'),

-- S010 孙雅 — 目标 产品/运营
(16,
 '[{"school":"广州职业技术学院","major":"计算机应用技术","degree":"大专","startYear":"2023-09","endYear":"2026-06"}]',
 '[{"company":"广州互联网科技有限公司","position":"产品助理实习生","start":"2025-08","end":"2025-12","description":"协助产品经理完成需求文档编写和原型图绘制，参与用户访谈并整理反馈报告"}]',
 'Axure, 墨刀, XMind, 文档编写, Excel, 基础SQL',
 '逻辑清晰，善于沟通。对产品设计有浓厚兴趣，喜欢从用户角度思考问题。实习期间独立完成了2份功能需求文档的编写。',
 '产品助理'),

-- 额外补充：再增加几条以使数据更完整
(6,
 '[{"school":"广东技术师范大学","major":"计算机科学与技术","degree":"本科","startYear":"2022-09","endYear":"2026-06"}]',
 '[{"company":"腾讯科技","position":"Java开发实习生","duration":"2025-07至2025-12"}]',
 'Java, Spring Boot, MySQL, Redis, 分布式基础',
 '热爱编程，有较强的学习能力和自驱力',
 'Java开发工程师');

-- ── 投递记录 ──
INSERT INTO `delivery` (`student_id`, `job_id`, `resume_version`, `status`, `interview_time`, `interview_location`, `feedback`, `create_time`) VALUES
-- 张明 -> Java后端开发工程师（已查看）
(7, 1, 'v1.0', 1, NULL, NULL, NULL, '2026-06-15 09:30:00'),
-- 李婷 -> 前端开发工程师（面试中，已安排面试）
(8, 2, 'v1.0', 2, '2026-07-05 14:00:00', '广州市天河区软件路15号3楼会议室', NULL, '2026-06-16 10:00:00'),
-- 王浩 -> Python数据分析师（待查看）
(9, 3, 'v1.0', 0, NULL, NULL, NULL, '2026-06-18 11:00:00'),
-- 陈雪 -> 软件测试工程师（已录用）
(10, 4, 'v1.0', 3, '2026-06-25 10:00:00', '深圳市南山区科技园南路8号', '同学面试表现优秀，欢迎加入！', '2026-06-10 14:00:00'),
-- 刘强 -> 嵌入式软件工程师（不合适）
(11, 5, 'v1.0', 4, NULL, NULL, '经验与该岗位要求匹配度不足', '2026-06-12 09:00:00'),
-- 张明 -> 硬件测试技术员（待面试）
(7, 8, 'v1.0', 2, '2026-07-06 15:00:00', '东莞市长安镇振安东路88号', NULL, '2026-06-20 16:30:00'),
-- 黄丽 -> 新媒体运营专员（待查看）
(12, 7, 'v1.0', 0, NULL, NULL, NULL, '2026-06-22 09:00:00'),
-- 周杰 -> 前端开发工程师（已投递）
(13, 2, 'v1.0', 0, NULL, NULL, NULL, '2026-06-23 14:00:00'),
-- 吴芳 -> 硬件测试技术员（面试中）
(14, 8, 'v1.0', 2, '2026-07-07 10:00:00', '东莞市长安镇振安东路88号', NULL, '2026-06-19 11:30:00'),
-- 郑鹏 -> Java后端开发工程师（已查看）
(15, 1, 'v1.0', 1, NULL, NULL, NULL, '2026-06-21 08:00:00'),
-- 孙雅 -> 产品助理实习（待查看）
(16, 6, 'v1.0', 0, NULL, NULL, NULL, '2026-06-24 15:00:00');

-- ── 收藏 ──
INSERT INTO `favorite` (`student_id`, `job_id`) VALUES
(7, 1), (7, 3), (8, 2), (9, 3), (12, 7), (16, 6);

-- ── 消息 ──
INSERT INTO `message` (`student_id`, `title`, `content`, `type`, `is_read`) VALUES
(7,  '投递成功', '您已成功投递「Java后端开发工程师」至广州创想科技有限公司，请耐心等待企业反馈', 2, 1),
(7,  '简历被查看', '您的简历已被广州创想科技有限公司查看', 0, 0),
(8,  '面试邀请', '您投递的「前端开发工程师」岗位已通过初筛，面试时间：2026年7月5日14:00，地点：广州市天河区软件路15号', 1, 0),
(10, '录用通知', '恭喜您通过「软件测试工程师」面试！深圳蓝桥信息技术有限公司已向您发出录用通知', 1, 1),
(10, '入职提醒', '您的入职时间为2026年7月15日，请提前准备好相关材料', 0, 0),
(11, '未通过通知', '您投递的「嵌入式软件工程师」岗位未通过筛选，建议完善简历后尝试其他岗位', 2, 1),
(12, '投递成功', '您已成功投递「新媒体运营专员」至佛山智造科技有限公司', 2, 0),
(14, '面试邀请', '您投递的「硬件测试技术员」岗位已通过初筛，面试时间：2026年7月7日10:00，地点：东莞市长安镇振安东路88号', 1, 0),
(9,  '系统通知', 'AI简历评分功能已上线，前往查看您的简历评分和岗位匹配建议', 0, 0);

-- ── 操作日志 ──
INSERT INTO `operation_log` (`user_id`, `operation_type`, `target_id`, `ip_address`) VALUES
(1, '查看简历', '7', '127.0.0.1'),
(4, '查看投递', '1', '127.0.0.1'),
(4, '安排面试', '8', '127.0.0.1'),
(5, '录用', '10', '127.0.0.1');

-- ── AI 解析日志（模拟已解析过的2条）──
INSERT INTO `ai_parse_log` (`teacher_id`, `raw_message`, `parsed_result`, `confidence_score`, `create_time`) VALUES
(1, '姓名：张三\n电话：13812345678\n邮箱：zhangsan@qq.com\n教育背景：XX职业学院计算机专业\n技能：Java,Spring,MySQL',
 '{"name":"张三","phone":"13812345678","email":"zhangsan@qq.com","school":"XX职业学院","major":"计算机专业","education":"大专","skills":["Java","Spring","MySQL"],"experience":"2年开发经验"}',
 0.88, '2026-06-20 10:00:00');

-- ── 人岗匹配记录（AI 已算过的几条）──
INSERT INTO `job_match_record` (`job_id`, `student_id`, `match_score`, `match_reason`, `is_pushed`, `create_time`) VALUES
-- S001 张明（Java方向）
(1, 1, 0.92, 'Java技能完全匹配，有Spring Boot和MySQL实习经验', 1, '2026-06-25 10:00:00'),
(2, 1, 0.65, '具有基础编程能力但前端经验不足', 0, '2026-06-25 10:00:00'),
(3, 1, 0.45, '和技术岗位匹配度较低', 0, '2026-06-25 10:00:00'),
-- S002 李婷（前端方向）
(1, 2, 0.60, '有编程基础但Java专项经验不足', 0, '2026-06-25 10:00:00'),
(2, 2, 0.90, 'Vue.js和JavaScript技能完全匹配', 1, '2026-06-25 10:00:00'),
(3, 2, 0.50, '和技术岗位有一定差距', 0, '2026-06-25 10:00:00'),
-- S003 王浩（Python方向）
(1, 3, 0.55, '编程基础良好但Java专项不足', 0, '2026-06-25 10:00:00'),
(2, 3, 0.50, '前端技能有待提升', 0, '2026-06-25 10:00:00'),
(3, 3, 0.88, 'Python和数据分析技能高度匹配', 1, '2026-06-25 10:00:00'),
-- S007 周杰
(1, 7, 0.88, 'Java技能匹配度高，有相关实习经验', 1, '2026-06-25 10:00:00'),
(2, 8, 0.85, 'Vue.js技能匹配，有前端实习经验', 1, '2026-06-25 10:00:00'),
(3, 9, 0.80, 'Python和数据分析基础良好', 0, '2026-06-25 10:00:00'),
-- HR侧已推送
(7, 12, 0.82, '有新媒体运营实习经验，内容创作能力突出', 0, '2026-06-25 10:00:00');
(8, 14, 0.86, '硬件测试实习经验匹配，专业对口', 0, '2026-06-26 14:00:00');

-- ── 简历评分记录（AI 已评过的几条）──
INSERT INTO `resume_score_log` (`job_id`, `delivery_id`, `score`, `score_detail`, `create_time`) VALUES
(1, 1, 86, '{"技能得分":88,"经验得分":82,"教育得分":85,"评语":"Java基础扎实，有实习经验加分"}', '2026-06-25 10:00:00'),
(2, 2, 82, '{"技能得分":85,"经验得分":78,"教育得分":82,"评语":"Vue技能掌握良好，实习经验相关度高"}', '2026-06-25 10:30:00'),
(8, 6, 75, '{"技能得分":70,"经验得分":72,"教育得分":82,"评语":"跨岗位投递，技能匹配度一般"}', '2026-06-26 15:00:00');

SELECT '✅ 数据库初始化完成！共创建15张表，插入测试数据。' AS result;
