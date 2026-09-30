package com.recruit.controller;

import com.recruit.entity.Job;
import com.recruit.entity.Resume;
import com.recruit.service.JobService;
import com.recruit.service.ResumeService;
import com.recruit.utils.AiService;
import com.recruit.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 学生向 AI 助手控制器（简历诊断 / 模拟面试练习）
 * 与教师向的 /ai-parse 分离：本控制器所有接口仅学生本人可调用，
 * 数据只允许访问自己的简历，不接收外部 studentId（防越权，参考 P0-2）
 */
@Slf4j
@RestController
@RequestMapping("/ai-assistant")
public class AiAssistantController extends BaseController {

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private JobService jobService;

    @Autowired
    private AiService aiService;

    /**
     * AI 简历诊断：分析当前登录学生自己的简历
     * 简历在最近诊断后未变更时直接返回缓存结果（cached=true），
     * force=true 强制重新调用 AI
     */
    @PostMapping("/resume-review")
    public Result<Map<String, Object>> resumeReview(@RequestBody(required = false) Map<String, Object> params) {
        requireStudent();

        boolean force = params != null && Boolean.parseBoolean(String.valueOf(params.get("force")));

        Resume resume = resumeService.selectByStudentId(getCurrentUserId());
        if (resume == null) {
            return Result.error(404, "请先完善简历后再使用 AI 诊断");
        }

        Map<String, Object> aiResult = resumeService.analyzeWithCache(resume, force);
        boolean cached = Boolean.TRUE.equals(aiResult.get("cached"));
        return Result.success(cached ? "简历未变更，已返回最近诊断结果" : "诊断完成", aiResult);
    }

    /**
     * 模拟面试：针对指定岗位生成面试题（默认 5 道）
     * 入参 {jobId}
     */
    @PostMapping("/interview/questions")
    public Result<Map<String, Object>> genQuestions(@RequestBody Map<String, Object> params) {
        requireStudent();

        Long jobId = Long.valueOf(params.get("jobId").toString());
        Job job = jobService.getById(jobId);
        if (job == null) {
            return Result.error(404, "岗位不存在");
        }

        // 简历可为空：没有简历也能练技术/行为题
        Resume resume = resumeService.selectByStudentId(getCurrentUserId());

        Map<String, Object> aiResult = aiService.generateInterviewQuestions(
                job.getTitle(), job.getDescription(), job.getRequirement(), buildResumeText(resume), 5);
        return Result.success("面试题生成成功", aiResult);
    }

    /**
     * 模拟面试：点评学生的作答
     * 入参 {jobId, question, answer}
     */
    @PostMapping("/interview/evaluate")
    public Result<Map<String, Object>> evaluateAnswer(@RequestBody Map<String, Object> params) {
        requireStudent();

        Long jobId = Long.valueOf(params.get("jobId").toString());
        String question = params.get("question") == null ? "" : params.get("question").toString();
        String answer = params.get("answer") == null ? "" : params.get("answer").toString();

        Job job = jobService.getById(jobId);
        if (job == null) {
            return Result.error(404, "岗位不存在");
        }
        if (answer.trim().isEmpty()) {
            return Result.error("请先作答再提交点评");
        }

        Resume resume = resumeService.selectByStudentId(getCurrentUserId());

        Map<String, Object> aiResult = aiService.evaluateAnswer(
                job.getTitle(), question, answer, buildResumeText(resume));
        return Result.success("点评完成", aiResult);
    }

    /**
     * 把简历实体拼成纯文本（供 AI prompt 用）
     */
    private String buildResumeText(Resume resume) {
        if (resume == null) {
            return "（学生暂未填写简历）";
        }
        return "教育背景：" + safe(resume.getEducation())
                + "\n实习经历：" + safe(resume.getInternship())
                + "\n项目经历：" + safe(resume.getProject())
                + "\n技能：" + safe(resume.getSkills())
                + "\n自我评价：" + safe(resume.getSelfEvaluation())
                + "\n求职意向：" + safe(resume.getJobTarget());
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
