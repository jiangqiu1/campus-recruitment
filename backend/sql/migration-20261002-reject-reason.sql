-- ============================================
-- 后端专项：审批拒绝原因字段
-- 日期: 2026-10-02
-- 背景: 审批拒绝此前不记录原因，教师无法说明、HR 无法查看
-- ============================================

ALTER TABLE `job_change_apply`
    ADD COLUMN `reject_reason` VARCHAR(200) DEFAULT NULL COMMENT '拒绝原因（教师填写）' AFTER `review_teacher_id`;
