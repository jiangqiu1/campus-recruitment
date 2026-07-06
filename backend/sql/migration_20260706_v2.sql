-- ============================================
-- 数据库迁移脚本 v2: 企业信息扩展 + 投递更新时间
-- 日期: 2026-07-06
-- ============================================

-- 1. 企业表新增字段
ALTER TABLE `company`
    ADD COLUMN `size`        VARCHAR(50)  DEFAULT NULL COMMENT '企业规模' AFTER `industry`,
    ADD COLUMN `city`        VARCHAR(50)  DEFAULT NULL COMMENT '所在城市' AFTER `size`,
    ADD COLUMN `description` TEXT         DEFAULT NULL COMMENT '公司简介' AFTER `city`,
    ADD COLUMN `license_url` VARCHAR(255) DEFAULT NULL COMMENT '营业执照URL' AFTER `contact_phone`;

-- 2. 投递表新增更新时间字段
ALTER TABLE `delivery`
    ADD COLUMN `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`;
