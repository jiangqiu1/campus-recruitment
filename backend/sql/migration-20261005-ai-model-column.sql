-- ai_parse_log 增加模型版本列（多模型对比实验精确到版本）
-- 执行时间：2026-10-05
ALTER TABLE ai_parse_log ADD COLUMN model VARCHAR(50) DEFAULT NULL COMMENT '模型版本（如 deepseek-chat/glm-4.5-flash）' AFTER provider;
