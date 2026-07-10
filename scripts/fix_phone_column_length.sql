-- sys_user 表 phone 字段长度不足（AES 加密后超出 VARCHAR(20)）
ALTER TABLE sys_user MODIFY COLUMN phone VARCHAR(100) DEFAULT NULL COMMENT '手机号（AES加密）';
