-- 为 sys_user 表添加学校/专业字段
ALTER TABLE `sys_user`
    ADD COLUMN `school` VARCHAR(100) DEFAULT NULL COMMENT '学校名称' AFTER `wechat_openid`,
    ADD COLUMN `major`  VARCHAR(100) DEFAULT NULL COMMENT '专业名称' AFTER `school`;
