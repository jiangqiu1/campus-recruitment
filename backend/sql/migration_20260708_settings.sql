-- ================================================
-- 系统设置持久化表
-- 用于存储系统级别配置，替代原有的内存 ConcurrentHashMap
-- ================================================

CREATE TABLE IF NOT EXISTS `sys_settings` (
    `id`            BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_key`     VARCHAR(50)  NOT NULL COMMENT '分组标识: basic/security/notification',
    `setting_key`   VARCHAR(100) NOT NULL COMMENT '配置键名',
    `setting_value` TEXT         DEFAULT NULL COMMENT '配置值（JSON字符串）',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_key` (`group_key`, `setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统设置表';

-- 初始化默认设置
INSERT INTO `sys_settings` (`group_key`, `setting_key`, `setting_value`) VALUES
('basic',       'systemName',  '"职业院校校企招聘与就业管理平台"'),
('basic',       'pageSize',    '20'),
('basic',       'logo',        '""'),
('security',    'jwtExpiration', '604800'),
('notification', 'emailEnabled', 'false'),
('notification', 'smtpHost',   '"smtp.example.com"'),
('notification', 'smtpPort',   '587'),
('notification', 'smtpUsername', '""'),
('notification', 'smtpPassword', '""')
ON DUPLICATE KEY UPDATE `setting_value` = VALUES(`setting_value`);
