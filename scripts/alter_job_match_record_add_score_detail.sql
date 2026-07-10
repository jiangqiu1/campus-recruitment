-- 人岗匹配记录表增加子维度分数字段
ALTER TABLE job_match_record ADD COLUMN score_detail TEXT COMMENT '子维度分数JSON（技能/学历/经验/专业等）';
