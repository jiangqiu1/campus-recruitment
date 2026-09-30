package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.recruit.entity.Job;
import com.recruit.entity.JobMatchRecord;
import com.recruit.entity.Resume;
import com.recruit.entity.SysUser;
import com.recruit.mapper.JobMapper;
import com.recruit.mapper.JobMatchRecordMapper;
import com.recruit.mapper.ResumeMapper;
import com.recruit.service.JobMatchRecordService;
import com.recruit.service.StudentClassService;
import com.recruit.service.UserService;
import com.recruit.utils.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class JobMatchRecordServiceImpl extends ServiceImpl<JobMatchRecordMapper, JobMatchRecord> implements JobMatchRecordService {

    private static final Logger log = LoggerFactory.getLogger(JobMatchRecordServiceImpl.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private JobMatchRecordMapper jobMatchRecordMapper;

    @Autowired
    private JobMapper jobMapper;

    @Autowired
    private ResumeMapper resumeMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private AiService aiService;

    @Autowired
    private com.recruit.service.StudentClassService studentClassService;

    @Override
    public List<JobMatchRecord> selectByJobIdOrderByScore(Long jobId) {
        return jobMatchRecordMapper.selectByJobIdOrderByScore(jobId);
    }

    @Override
    public List<JobMatchRecord> selectByStudentId(Long studentId) {
        return jobMatchRecordMapper.selectByStudentId(studentId);
    }

    @Override
    public List<JobMatchRecord> selectPushedByJobId(Long jobId) {
        return jobMatchRecordMapper.selectPushedByJobId(jobId);
    }

    @Override
    public List<JobMatchRecord> selectClickedByJobId(Long jobId) {
        return jobMatchRecordMapper.selectClickedByJobId(jobId);
    }

    @Override
    public List<JobMatchRecord> selectByScoreRange(BigDecimal minScore, BigDecimal maxScore) {
        return jobMatchRecordMapper.selectByScoreRange(minScore, maxScore);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean generateMatchRecord(Long jobId, Long studentId) {
        // 如果存在旧记录则先删除（支持重新匹配）
        this.remove(Wrappers.<JobMatchRecord>lambdaQuery()
                .eq(JobMatchRecord::getJobId, jobId)
                .eq(JobMatchRecord::getStudentId, studentId));

        Job job = jobMapper.selectById(jobId);
        Resume resume = resumeMapper.selectByStudentId(studentId);
        SysUser student = studentId != null ? userService.getById(studentId) : null;

        String jobTitle = job != null ? job.getTitle() : "";
        String jobDesc = job != null ? job.getDescription() : "";
        String jobReq = job != null ? job.getRequirement() : "";
        String resumeText = buildResumeText(resume, student);

        Map<String, Object> result = aiService.matchJob(jobTitle, jobDesc, jobReq,
                student != null ? student.getRealName() : "未知", resumeText);

        int matchScoreInt = toInt(result.get("matchScore"));
        BigDecimal matchScore = new BigDecimal(matchScoreInt).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        String matchReason = result.getOrDefault("matchReason", "").toString();

        // 构建子维度 JSON
        String scoreDetail = null;
        try {
            Map<String, Object> detail = new java.util.LinkedHashMap<>();
            if (result.containsKey("skillMatch")) detail.put("skillMatch", toInt(result.get("skillMatch")));
            if (result.containsKey("eduMatch")) detail.put("eduMatch", toInt(result.get("eduMatch")));
            if (result.containsKey("expMatch")) detail.put("expMatch", toInt(result.get("expMatch")));
            if (result.containsKey("majorFit")) detail.put("majorFit", toInt(result.get("majorFit")));
            if (!detail.isEmpty()) {
                scoreDetail = OBJECT_MAPPER.writeValueAsString(detail);
            }
        } catch (Exception e) {
            log.warn("序列化 scoreDetail 失败", e);
        }

        JobMatchRecord record = new JobMatchRecord();
        record.setJobId(jobId);
        record.setStudentId(studentId);
        record.setMatchScore(matchScore);
        record.setMatchReason(matchReason);
        record.setScoreDetail(scoreDetail);
        record.setIsPushed(0);
        record.setIsClicked(0);
        record.setCreateTime(LocalDateTime.now());

        return save(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchGenerateMatchRecords(Long jobId, Long classId) {
        // 获取待匹配的学生ID列表
        List<Long> targetStudentIds;
        if (classId != null) {
            // 按班级筛选
            List<com.recruit.entity.StudentClass> scList = studentClassService.selectByClassId(classId);
            targetStudentIds = scList.stream()
                    .map(com.recruit.entity.StudentClass::getStudentId)
                    .collect(java.util.stream.Collectors.toList());
        } else {
            // 全量匹配：从 resume 表取所有有简历的学生
            targetStudentIds = resumeMapper.selectList(null)
                    .stream().map(Resume::getStudentId)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());
        }

        // 排除已有匹配记录的学生
        List<Long> existingStudentIds = jobMatchRecordMapper.selectByJobIdOrderByScore(jobId)
                .stream().map(JobMatchRecord::getStudentId).collect(java.util.stream.Collectors.toList());

        int count = 0;
        for (Long studentId : targetStudentIds) {
            if (existingStudentIds.contains(studentId)) continue;
            try {
                if (generateMatchRecord(jobId, studentId)) count++;
            } catch (Exception e) {
                log.error("批量匹配失败: studentId={}", studentId, e);
            }
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePushedStatus(Long recordId) {
        int rows = jobMatchRecordMapper.updatePushedStatus(recordId, LocalDateTime.now());
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateClickedStatus(Long recordId) {
        int rows = jobMatchRecordMapper.updateClickedStatus(recordId);
        return rows > 0;
    }

    @Override
    public BigDecimal calculatePushRate(Long jobId) {
        List<JobMatchRecord> allRecords = jobMatchRecordMapper.selectByJobIdOrderByScore(jobId);
        if (allRecords.isEmpty()) return BigDecimal.ZERO;
        long pushedCount = allRecords.stream().filter(r -> r.getIsPushed() == 1).count();
        return new BigDecimal(pushedCount).divide(new BigDecimal(allRecords.size()), 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateClickRate(Long jobId) {
        List<JobMatchRecord> pushedRecords = jobMatchRecordMapper.selectPushedByJobId(jobId);
        if (pushedRecords.isEmpty()) return BigDecimal.ZERO;
        long clickedCount = pushedRecords.stream().filter(r -> r.getIsClicked() == 1).count();
        return new BigDecimal(clickedCount).divide(new BigDecimal(pushedRecords.size()), 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateAverageMatchScore(Long jobId) {
        List<JobMatchRecord> records = jobMatchRecordMapper.selectByJobIdOrderByScore(jobId);
        if (records.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (JobMatchRecord record : records) {
            sum = sum.add(record.getMatchScore());
        }
        return sum.divide(new BigDecimal(records.size()), 2, RoundingMode.HALF_UP);
    }

    // ========== 工具方法 ==========

    @Override
    public int deleteByJobId(Long jobId) {
        return jobMatchRecordMapper.deleteByJobId(jobId);
    }

    private String buildResumeText(Resume resume, SysUser student) {
        if (resume == null) return "暂无简历数据";
        StringBuilder sb = new StringBuilder();
        if (student != null && student.getRealName() != null) sb.append("姓名：").append(student.getRealName()).append("\n");
        if (resume.getEducation() != null) sb.append("教育：").append(resume.getEducation()).append("\n");
        if (resume.getSkills() != null) sb.append("技能：").append(resume.getSkills()).append("\n");
        if (resume.getInternship() != null) sb.append("实习：").append(resume.getInternship()).append("\n");
        if (resume.getSelfEvaluation() != null) sb.append("自评：").append(resume.getSelfEvaluation()).append("\n");
        if (resume.getJobTarget() != null) sb.append("求职意向：").append(resume.getJobTarget()).append("\n");
        return sb.toString();
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return 0; }
    }
}
