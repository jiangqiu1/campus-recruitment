# 简历模块 · 简历与 AI 解析详解（深度补充）

> 本文档基于 `ResumeController.java` 源码，记录简历的两种录入方式、数据级权限与已知问题。

## 1. 简历的两种录入方式

| 方式 | 入口 | 流程 |
|------|------|------|
| 结构化编辑 | `createOrUpdateResume`（99 行） | 前端表单直接存 education/internship/project/skills 等字段 |
| PDF + AI 解析 | `uploadAndParse`（184 行） | 上传 PDF → PDFBox 提文本 → AI 抽 16 字段 → 回填 |

## 2. 结构化保存流程（createOrUpdateResume，99 行）

```
① 同步更新用户基本信息（name/gender/phone/email）→ userService.updateById
② 组装 Resume 对象（studentId + education/internship/project/skills/selfEvaluation/jobTarget）
③ 有 id 则 updateById，无 id 则 save（新建）
```

**注意**：第 ② 步保存简历时，`education`/`internship`/`project` 是 JSON 字符串直接存（前端序列化后传入）。

## 3. PDF 上传 + AI 解析（uploadAndParse，184 行）

```
① aiService.extractTextFromPdf(file)     PDFBox 提取文本，空则报"非文字版PDF"
② aiService.parseResume(rawText)         AI 抽取 16 字段（详见 AI 模块 Prompt 规范）
③ 保存 pdfUrl 到 resume
④ 返回 { parsedData, pdfUrl }
```

> `uploadPdf`（150 行）是**模拟路径**（TODO），只生成 `/uploads/resume/resume_{studentId}.pdf` 字符串，未真正落盘。

## 4. 数据级权限（getStudentResume，325 行，已修复 2026-08-31）

按角色分级校验：

```java
if (role == 1) {            // 教师：遍历自己的班级，判断学生是否在本班
    if (!inMyClass) return 403;
} else if (role == 0) {     // 学生：只能查看自己的简历
    if (!studentId.equals(currentUserId)) return 403;
} else if (role == 2) {     // HR：只能查看投递本企业岗位的学生
    // 查本企业岗位 → 查投递这些岗位的学生 → 判断 studentId 是否在其中
    if (!allowed) return 403;
}
// 管理员（role=3）：无限制
```

教师通过「班级 → 学生」链路校验（复用班级模块的权限链）；学生/HR 补齐了校验，管理员不受限。

## 5. 默认简历机制

- `setDefaultResume`（234 行）：设置某简历为默认。
- `getMyDefaultResume`（82 行）：投递时取默认简历。

## 6. 已知问题（重要）

1. **手机号明文存储**（已修复 2026-08-31）：`createOrUpdateResume` 原第 112 行 `user.setPhone(phone.toString())` 未加密，现已改为 `user.setPhone(aesUtil.encrypt(phone.toString()))`（现第 116 行），并注入 `AESUtil`、补 import，与 `AuthController.updateProfile` 保持一致。
2. **越权访问隐患**（已修复 2026-08-31）：`getMyResume` 已改为强制使用当前登录 userId，忽略外部传入的 `studentId`。
3. **getStudentResume 非教师无校验**（已修复 2026-08-31）：已补全角色校验——学生只能查自己、HR 只能查投递本企业岗位的学生（新增注入 JobService/DeliveryService）、管理员不受限。
4. **parseResume 返回模拟数据**：`resumeService.parseResumePdf` / `updateSkillTags`（260-263 行）为占位实现，未真正解析。
