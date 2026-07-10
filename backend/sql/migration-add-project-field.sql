-- resume 表新增 project 字段（JSON数组，与 internship 结构一致）
ALTER TABLE `resume`
    ADD COLUMN `project` TEXT DEFAULT NULL COMMENT '项目经历（JSON数组）' AFTER `internship`;
