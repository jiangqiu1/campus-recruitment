-- ============================================
-- AI 双模型接入：ai_parse_log 增加调用元数据字段
-- 日期: 2026-09-30
-- 用途: 记录每次 AI 调用的提供方/任务/耗时/是否降级，
--       支撑毕设"多模型对比实验"的数据采集
-- ============================================

ALTER TABLE `ai_parse_log`
    ADD COLUMN `provider` VARCHAR(20) DEFAULT NULL COMMENT 'AI提供方: deepseek/glm' AFTER `teacher_id`,
    ADD COLUMN `task_name` VARCHAR(50) DEFAULT NULL COMMENT 'AI任务名: scoreResume/matchJob/parseResume/parseJob/analyzeResume/genQuestions/evalAnswer' AFTER `provider`,
    ADD COLUMN `latency_ms` INT DEFAULT NULL COMMENT '调用耗时（毫秒）' AFTER `task_name`,
    ADD COLUMN `user_id` BIGINT DEFAULT NULL COMMENT '发起用户ID（新调用统一记录，teacher_id 仅旧解析数据使用）' AFTER `latency_ms`,
    ADD COLUMN `mock_flag` TINYINT(1) DEFAULT 0 COMMENT '是否降级为mock数据: 0=真实调用 1=降级mock' AFTER `user_id`,
    -- teacher_id 原为 NOT NULL（旧表结构），新调用统一记 user_id，需放开为可空
    MODIFY COLUMN `teacher_id` BIGINT DEFAULT NULL COMMENT '发起解析的教师ID（仅旧版解析数据使用）';
