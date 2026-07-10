-- ============================================================
-- 修复已有投递记录的面试时间与地点为空的问题
-- 针对状态 >= 2（面试中/已通过/未通过）的已有数据
-- ============================================================

-- 1. 给状态=2（面试中）的记录设置未来面试时间，避免为过去时间
-- 以 create_time + 8天 作为面试时间，统一安排地点
UPDATE `delivery`
SET
  `interview_time` = DATE_ADD(`create_time`, INTERVAL 8 DAY),
  `interview_location` = COALESCE(`interview_location`, '深圳市南山区科技园')
WHERE `status` = 2 AND `interview_time` IS NULL;

-- 2. 给状态=3（已通过）的记录设置面试时间为 create_time + 1天（已结束）
UPDATE `delivery`
SET
  `interview_time` = DATE_ADD(`create_time`, INTERVAL 1 DAY),
  `interview_location` = COALESCE(`interview_location`, '广州市天河区软件园')
WHERE `status` = 3 AND `interview_time` IS NULL;

-- 3. 给状态=4（未通过）的记录设置面试时间为 create_time + 1天（已结束）
UPDATE `delivery`
SET
  `interview_time` = DATE_ADD(`create_time`, INTERVAL 1 DAY),
  `interview_location` = COALESCE(`interview_location`, '线上视频面试（腾讯会议）')
WHERE `status` = 4 AND `interview_time` IS NULL;
