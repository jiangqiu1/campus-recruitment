-- 教师 T001 测试数据（T001 userId = 2）
-- 用于验证教师首页 Dashboard 卡片统计
-- 执行前请确认当前用户 ID 为 2，岗位 4/5/6 由 T001 创建

-- =========================================================
-- 1. 待审批（job_change_apply）
-- 2 条状态为 0 且审核教师 review_teacher_id=2 的变更申请
-- =========================================================
INSERT INTO `job_change_apply` (`job_id`, `hr_id`, `change_content`, `status`, `review_teacher_id`, `create_time`) VALUES
(4, 5, '{"title": "软件测试工程师（高级）", "salaryRange": "10K-16K"}', 0, 2, NOW()),
(5, 5, '{"location": "深圳", "description": "新增工作地点深圳"}', 0, 2, NOW());

-- =========================================================
-- 2. 未读简历（delivery status=0）
-- 3 条投递到 T001 创建的岗位 4/5/6，状态=0（已投递/未查看）
-- =========================================================
INSERT INTO `delivery` (`student_id`, `job_id`, `resume_version`, `status`, `create_time`) VALUES
(6, 4, 'latest', 0, NOW()),
(7, 5, 'latest', 0, NOW()),
(8, 6, 'latest', 0, NOW());

-- =========================================================
-- 3. 今日新增投递（delivery 创建时间为今天）
-- 2 条今日投递，分别状态 0 和 1
-- =========================================================
INSERT INTO `delivery` (`student_id`, `job_id`, `resume_version`, `status`, `create_time`) VALUES
(9, 4, 'latest', 0, NOW()),
(10, 5, 'latest', 1, NOW());

-- =========================================================
-- 4. 历史投递（用于增加投递总数，不是今日）
-- =========================================================
INSERT INTO `delivery` (`student_id`, `job_id`, `resume_version`, `status`, `create_time`) VALUES
(11, 4, 'latest', 1, '2026-07-01 10:00:00'),
(12, 5, 'latest', 2, '2026-07-02 11:00:00'),
(13, 6, 'latest', 3, '2026-07-03 12:00:00');

-- =========================================================
-- 5. 清理脚本（如需要恢复测试前状态，取消注释并执行）
-- =========================================================
-- DELETE FROM `job_change_apply` WHERE `review_teacher_id` = 2 AND `status` = 0;
-- DELETE FROM `delivery` WHERE `student_id` IN (6,7,8,9,10,11,12,13) AND `job_id` IN (4,5,6);
