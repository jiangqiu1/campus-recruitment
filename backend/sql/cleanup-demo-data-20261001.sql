-- ============================================
-- 演示数据清理：通知去重 / 时间矛盾修复 / 面试排期 / 孤儿记录
-- 日期: 2026-10-01
-- 背景: 界面评审指出测试数据问题（重复通知、面试时间早于投递、
--       岗位删除后匹配记录残留导致「岗位#N」裸标题）
-- 注意: 仅清理演示数据，不影响表结构
-- ============================================

-- 1. 投递成功通知去重：每个学生仅保留最新一条
DELETE m FROM message m
LEFT JOIN (
    SELECT MAX(id) AS keep_id FROM message
    WHERE title LIKE '投递成功%' AND deleted = 0
    GROUP BY student_id
) k ON m.id = k.keep_id
WHERE m.title LIKE '投递成功%' AND k.keep_id IS NULL;

-- 2. 面试时间矛盾修复：面试时间早于投递时间的，顺延为投递后 2 天
UPDATE delivery
SET interview_time = DATE_ADD(create_time, INTERVAL 2 DAY)
WHERE interview_time IS NOT NULL AND interview_time < create_time;

-- 3. 面试中的投递：面试时间统一排期到未来 1~5 天（演示「下一场面试」）
UPDATE delivery
SET interview_time = DATE_ADD(NOW(), INTERVAL 1 + (id MOD 5) DAY)
WHERE status = 2 AND deleted = 0;

-- 4. 过旧的消息时间刷新到近 7 天（按 id 错开，避免同刻）
UPDATE message
SET create_time = DATE_SUB(NOW(), INTERVAL (id MOD 7) DAY)
WHERE create_time < DATE_SUB(NOW(), INTERVAL 30 DAY) AND deleted = 0;

-- 5. 孤儿匹配记录清理：岗位已删除/不可见的匹配记录
DELETE jm FROM job_match_record jm
LEFT JOIN job j ON jm.job_id = j.id AND j.deleted = 0
WHERE j.id IS NULL;
