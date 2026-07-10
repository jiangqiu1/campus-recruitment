-- ================================================
-- 通用消息通知表（非学生端消息，如管理员/教师/HR）
-- 独立于原有的 message 表（仅学生端）
-- ================================================

CREATE TABLE IF NOT EXISTS `user_message` (
    `id`          BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `user_id`     BIGINT(20)   NOT NULL COMMENT '接收者用户ID',
    `title`       VARCHAR(200) NOT NULL COMMENT '消息标题',
    `content`     TEXT         DEFAULT NULL COMMENT '消息内容',
    `type`        VARCHAR(50)  DEFAULT 'system' COMMENT '消息类型: system/audit/delivery/interview',
    `is_read`     TINYINT(1)   DEFAULT 0 COMMENT '是否已读: 0=未读, 1=已读',
    `related_id`  BIGINT(20)   DEFAULT NULL COMMENT '关联对象ID',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`),
    KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用消息通知表';
