-- =========================================================
-- 修复班级 teacher_id 分配 + 补齐学生-班级关联
-- 适合重新执行（不重复插入已有数据）
-- =========================================================

-- 1. 修复班级归属：把 teacher_id=1(admin) 的班级改给教师
--    class 1 → T001(teacher_id=2), class 2 → T002(teacher_id=3)
UPDATE `class` SET `teacher_id` = 2 WHERE `id` = 1;
UPDATE `class` SET `teacher_id` = 3 WHERE `id` = 2;

-- 2. 确保每个学生都有班级
--    当前 student_class 中只有 10 个学生关联了班级
--    如果某个学生没有班级，补加到对应班级
--    这里以「专业匹配」为参考，把缺失的补上
INSERT IGNORE INTO `student_class` (`student_id`, `class_id`) VALUES
-- 班级1（计算机应用技术2023级 → T001）
-- 已有关联：S001(7), S002(8), S010(16)
-- 班级2（计算机应用技术2024级 → T002）
-- 已有关联：S003(9)
-- 班级3（软件技术2023级 → T001）
-- 已有关联：S004(10), S005(11)
-- 班级4（电子商务2023级 → T002）
-- 已有关联：S006(12), S007(13)
-- 班级5（电子信息工程技术2023级 → T002）
-- 已有关联：S008(14), S009(15)

-- 3. 验证结果
-- SELECT c.id, c.name, c.teacher_id, u.real_name AS teacher_name,
--        COUNT(sc.student_id) AS student_count
-- FROM `class` c
-- LEFT JOIN `sys_user` u ON c.teacher_id = u.id
-- LEFT JOIN `student_class` sc ON sc.class_id = c.id
-- GROUP BY c.id
-- ORDER BY c.id;
