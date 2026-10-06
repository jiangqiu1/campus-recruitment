-- company.contact_phone 加宽以容纳 AES 密文（v1:IV32hex+密文hex，约67字符）
-- 执行时间：2026-10-05
ALTER TABLE company MODIFY COLUMN contact_phone VARCHAR(100) DEFAULT NULL COMMENT '联系电话（AES加密存储）';
