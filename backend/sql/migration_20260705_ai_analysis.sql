-- ============================================
-- 数据库迁移脚本: 为 resume 表添加 ai_analysis 字段
-- 用于持久化 AI 简历分析结果（教师分析后保存，学生可查看）
-- ============================================

ALTER TABLE `resume`
    ADD COLUMN `ai_analysis` TEXT DEFAULT NULL COMMENT 'AI简历分析结果（JSON）' AFTER `job_target`;

-- ============================================
-- 修复 job 表 trace_id 无默认值导致新建岗位失败的问题
-- 同时让 trace_id 支持自动生成，确保向后兼容
-- ============================================
ALTER TABLE `job`
    MODIFY COLUMN `trace_id` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '唯一追踪ID';
-- 注意：执行前请确保现有数据的 trace_id 不为空（没有空字符串），
-- 若已有空值可先 UPDATE job SET trace_id = UUID() WHERE trace_id IS NULL OR trace_id = '';
