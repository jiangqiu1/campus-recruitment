-- ====================================
-- 职业院校校企招聘与就业管理平台
-- 测试数据初始化脚本（已修复）
-- ====================================

USE `campus_recruitment`;

-- ====================================
-- 4.1 用户测试数据（sys_user）
-- 密码统一为：123456（BCrypt加密）
-- 修复：管理员 phone 字段改为 NULL（原填入了邮箱）
-- ====================================
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `phone`, `status`) VALUES
-- 管理员（phone 改为 NULL，因为管理员不一定有手机号）
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 3, NULL, 1),

-- 教师
('T001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '张老师', 1, '13800000001', 1),
('T002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '李老师', 1, '13800000002', 1),

-- 企业HR
('HR001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '王经理', 2, '13900000001', 1),
('HR002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '赵主管', 2, '13900000002', 1),

-- 学生
('S001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '小明', 0, '13600000001', 1),
('S002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '小红', 0, '13600000002', 1),
('S003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '小华', 0, '13600000003', 1),
('S004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '小丽', 0, '13600000004', 1),
('S005', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EH', '小强', 0, '13600000005', 1);


-- ====================================
-- 4.2 企业测试数据（company）
-- 修复：contact_phone 改为 NULL（原使用了明文占位符）
-- 注意：实际项目中应使用 AES 加密，这里用 NULL 避免加密解析失败
-- ====================================
INSERT INTO `company` (`name`, `short_name`, `industry`, `address`, `contact_person`, `contact_phone`, `cooperation_level`) VALUES
('腾讯科技有限公司', '腾讯', '互联网', '深圳市南山区', '马总', NULL, 2),
('阿里巴巴集团', '阿里', '互联网', '杭州市余杭区', '张总', NULL, 2),
('华为技术有限公司', '华为', '通信/硬件', '深圳市龙岗区', '任正非', NULL, 1),
('字节跳动科技有限公司', '字节', '互联网', '北京市海淀区', '张一鸣', NULL, 1),
('比亚迪股份有限公司', '比亚迪', '汽车/新能源', '深圳市坪山区', '王传福', NULL, 0);


-- ====================================
-- 4.3 岗位测试数据（job）
-- 修复：trace_id 使用 REPLACE(UUID(), '-', '') 生成 32 位字符串
-- ====================================
INSERT INTO `job` (`company_id`, `title`, `salary_range`, `education`, `location`, `description`, `requirement`, `status`, `created_by`, `trace_id`, `ai_keywords`, `required_skills`) VALUES
(1, 'Java开发工程师', '10k-20k', '本科', '深圳', '负责后端服务开发与维护', '熟练掌握Java、Spring Boot、MySQL', 1, 1, REPLACE(UUID(), '-', ''), '["Java", "Spring", "MySQL"]', '["Java", "Spring Boot", "MySQL", "Redis"]'),
(1, '前端开发工程师', '10k-18k', '本科', '深圳', '负责Web前端开发', '熟练掌握Vue.js、React、TypeScript', 1, 1, REPLACE(UUID(), '-', ''), '["Vue", "React", "TypeScript"]', '["Vue.js", "React", "TypeScript", "Webpack"]'),
(2, 'Python开发工程师', '12k-22k', '本科', '杭州', '负责数据平台开发', '熟练掌握Python、Django、数据处理', 1, 1, REPLACE(UUID(), '-', ''), '["Python", "Django", "Data"]', '["Python", "Django", "Pandas", "NumPy"]'),
(3, '嵌入式软件工程师', '15k-25k', '硕士', '深圳', '负责嵌入式系统开发', '熟练掌握C/C++、嵌入式Linux', 1, 2, REPLACE(UUID(), '-', ''), '["C", "C++", "Embedded"]', '["C", "C++", "Linux", "ARM"]'),
(4, '算法工程师', '20k-35k', '硕士', '北京', '负责推荐算法开发', '熟练掌握机器学习、深度学习', 1, 2, REPLACE(UUID(), '-', ''), '["ML", "DL", "Algorithm"]', '["Python", "TensorFlow", "PyTorch", "ML"]'),
(5, '硬件测试工程师', '8k-15k', '本科', '深圳', '负责硬件产品测试', '熟悉硬件测试流程、测试工具', 1, 1, REPLACE(UUID(), '-', ''), '["Test", "Hardware"]', '["硬件测试", "示波器", "万用表"]');


-- ====================================
-- 4.4 班级测试数据（class）
-- ====================================
INSERT INTO `class` (`name`, `teacher_id`, `major`, `grade`) VALUES
('计算机科学与技术2023级1班', 1, '计算机科学与技术', '2023级'),
('软件工程2023级1班', 1, '软件工程', '2023级'),
('电子信息工程2023级1班', 2, '电子信息工程', '2023级');


-- ====================================
-- 4.5 学生-班级关联（student_class）
-- ====================================
INSERT INTO `student_class` (`student_id`, `class_id`) VALUES
(6, 1),  -- 小明 -> 计算机1班
(7, 1),  -- 小红 -> 计算机1班
(8, 2),  -- 小华 -> 软件工程1班
(9, 2),  -- 小丽 -> 软件工程1班
(10, 3); -- 小强 -> 电子信息1班


-- ====================================
-- 4.6 简历测试数据（resume）
-- ====================================
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


-- ====================================
-- 4.7 投递记录测试数据（delivery）
-- ====================================
INSERT INTO `delivery` (`student_id`, `job_id`, `resume_version`, `status`) VALUES
(6, 1, 'v1.0', 2),  -- 小明 -> Java工程师（待面试）
(6, 2, 'v1.0', 1),  -- 小明 -> 前端工程师（已查看）
(7, 2, 'v1.0', 3),  -- 小红 -> 前端工程师（已录用）
(8, 3, 'v1.0', 0),  -- 小华 -> Python工程师（已投递）
(9, 4, 'v1.0', 1),  -- 小丽 -> 嵌入式工程师（已查看）
(10, 6, 'v1.0', 2); -- 小强 -> 硬件测试工程师（待面试）


-- ====================================
-- 4.8 人岗匹配测试数据（job_match_record）
-- ====================================
INSERT INTO `job_match_record` (`job_id`, `student_id`, `match_score`, `match_reason`, `is_pushed`) VALUES
(1, 6, 0.92, '技能匹配：Java, Spring Boot, MySQL', 1),
(2, 7, 0.88, '技能匹配：Vue.js, React, TypeScript', 1),
(3, 8, 0.85, '技能匹配：Python, Django', 1),
(4, 9, 0.90, '技能匹配：C, C++, Linux', 0),
(5, 10, 0.78, '技能匹配：硬件测试', 1);


-- ====================================
-- 4.9 简历评分测试数据（resume_score_log）
-- ====================================
INSERT INTO `resume_score_log` (`job_id`, `delivery_id`, `score`, `score_detail`) VALUES
(1, 1, 88, '{"技能得分":90,"项目经验":85,"教育背景":88,"自我评价":85}'),
(2, 2, 85, '{"技能得分":88,"项目经验":82,"教育背景":85,"自我评价":85}'),
(3, 4, 82, '{"技能得分":85,"项目经验":80,"教育背景":82,"自我评价":80}');


-- ====================================
-- 4.10 操作日志测试数据（operation_log）
-- ====================================
INSERT INTO `operation_log` (`user_id`, `operation_type`, `target_id`, `ip_address`) VALUES
(1, '查看简历', '6', '127.0.0.1'),
(1, '下载简历', '7', '127.0.0.1'),
(2, '发布岗位', '1', '127.0.0.1'),
(3, '查看投递', '1', '127.0.0.1');


-- ====================================
-- 完成提示
-- ====================================
SELECT '测试数据初始化完成！（已修复 trace_id、phone、contact_phone 问题）' AS message;
