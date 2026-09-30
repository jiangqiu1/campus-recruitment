<template>
  <el-dialog
    v-model="localVisible"
    title="简历详情"
    width="84vw"
    top="4vh"
    :close-on-click-modal="false"
    destroy-on-close
    append-to-body
    modal-class="resume-overlay"
    class="resume-dialog"
  >
    <div v-if="selectedStudent" class="resume-detail">
      <!-- 简历内容 -->
      <div class="resume-content">
        <div class="info-header-card">
          <el-avatar :size="60" style="background:linear-gradient(135deg,#165DFF,#2563EB);font-size:24px;font-weight:700;flex-shrink:0">
            {{ (resumeName).charAt(0) }}
          </el-avatar>
          <div class="basic-info">
            <h3 class="name">{{ resumeName }}</h3>
            <p class="desc">{{ resumeMajor }} · {{ resumeSchool }}</p>
            <div class="contact-row">
              <span>手机: {{ resumePhone }}</span>
              <span>邮箱: {{ resumeEmail }}</span>
            </div>
          </div>
        </div>

        <div v-if="resumeTags.length" class="tag-row">
          <span v-for="(t, i) in resumeTags" :key="i" class="tag-item">{{ t }}</span>
        </div>

        <div v-if="parsedResume.jobTarget" class="section-card">
          <div class="section-title">求职意向</div>
          <p class="section-text">{{ parsedResume.jobTarget }}</p>
        </div>

        <div class="section-card">
          <div class="section-title">教育经历</div>
          <div v-if="parsedEducation.length" class="exp-list">
            <div v-for="(edu, i) in parsedEducation" :key="i" class="exp-item">
              <div class="exp-head">
                <span class="exp-name">{{ edu.school }}</span>
                <span class="exp-time">{{ edu.startDate || edu.start }} - {{ edu.endDate || edu.end || '至今' }}</span>
              </div>
              <p class="exp-sub">{{ edu.major }} · {{ edu.degree }}</p>
            </div>
          </div>
          <p v-else class="empty-text">暂无教育经历</p>
        </div>

        <div class="section-card">
          <div class="section-title">实习经历</div>
          <div v-if="parsedInternship.length" class="exp-list">
            <div v-for="(job, i) in parsedInternship" :key="i" class="exp-item">
              <div class="exp-head">
                <span class="exp-name">{{ job.company || job.companyName }}</span>
                <span class="exp-time">{{ job.duration || job.start }} - {{ job.end || '至今' }}</span>
              </div>
              <p class="exp-sub">{{ job.position || job.jobTitle }}</p>
              <p v-if="job.description" class="exp-desc">{{ job.description }}</p>
            </div>
          </div>
          <p v-else class="empty-text">暂无实习经历</p>
        </div>

        <div class="section-card">
          <div class="section-title">项目经历</div>
          <div v-if="parsedProject.length" class="exp-list">
            <div v-for="(proj, i) in parsedProject" :key="i" class="exp-item">
              <div class="exp-head">
                <span class="exp-name">{{ proj.name }}</span>
                <span class="exp-time">{{ proj.duration || '' }}</span>
              </div>
              <p class="exp-sub">{{ proj.role || '' }}</p>
              <p v-if="proj.description" class="exp-desc">{{ proj.description }}</p>
            </div>
          </div>
          <p v-else class="empty-text">暂无项目经历</p>
        </div>

        <div class="section-card">
          <div class="section-title">技能证书</div>
          <div v-if="parsedSkills.length" class="skill-tags">
            <span v-for="(s, i) in parsedSkills" :key="i" class="skill-tag">{{ s }}</span>
          </div>
          <p v-else class="empty-text">暂无技能</p>
        </div>

        <div v-if="parsedResume.selfEvaluation" class="section-card">
          <div class="section-title">自我评价</div>
          <p class="section-text">{{ parsedResume.selfEvaluation }}</p>
        </div>
      </div>

      <!-- AI 分析侧栏 -->
      <div class="resume-ai-side">
        <div class="ai-sticky">
          <!-- 教师模式：AI 简历综合评分（来自 aiAnalysis） -->
          <template v-if="useTeacherAI">
            <div class="ai-score-card">
              <div class="ai-score-title">AI 综合评分</div>
              <div class="ai-score-circle">
                <span class="ai-score-num" :style="{ color: aiScoreColor }">{{ aiAnalysisData.overallScore || '--' }}</span>
                <span class="ai-score-total">/100</span>
              </div>
            </div>
            <div class="ai-details">
              <div v-if="aiAnalysisData.strengths?.length" class="ai-section">
                <div class="ai-subtitle">优势</div>
                <p v-for="(s, i) in aiAnalysisData.strengths" :key="i" class="ai-item ai-item--green">{{ s }}</p>
              </div>
              <div v-if="aiAnalysisData.weaknesses?.length" class="ai-section">
                <div class="ai-subtitle">不足</div>
                <p v-for="(w, i) in aiAnalysisData.weaknesses" :key="i" class="ai-item ai-item--red">{{ w }}</p>
              </div>
              <div v-if="aiAnalysisData.suggestions?.length" class="ai-section">
                <div class="ai-subtitle">改进建议</div>
                <p v-for="(sg, i) in aiAnalysisData.suggestions" :key="i" class="ai-item ai-item--blue">{{ sg }}</p>
              </div>
              <div v-if="aiAnalysisData.missingFields?.length" class="ai-section">
                <div class="ai-subtitle">缺失字段</div>
                <p class="ai-item ai-item--amber">{{ aiAnalysisData.missingFields.join('、') }}</p>
              </div>
              <div v-if="aiAnalysisData.recommendedSkills?.length" class="ai-section">
                <div class="ai-subtitle">推荐补充技能</div>
                <div class="ai-tags">
                  <span v-for="(sk, i) in aiAnalysisData.recommendedSkills" :key="i" class="ai-tag">{{ sk }}</span>
                </div>
              </div>
            </div>
          </template>

          <!-- HR 模式：岗位匹配度评分（来自 delivery） -->
          <template v-else-if="useHrAI && hrScore !== null && hrScore !== undefined">
            <div class="ai-score-card">
              <div class="ai-score-title">岗位匹配度</div>
              <div class="hr-score-hero">
                <div class="hr-score-circle">
                  <span class="hr-score-big" :style="{ color: aiScoreColor }">{{ hrScore }}</span>
                </div>
                <div class="hr-score-info">
                  <span class="hr-score-level" :style="{ color: aiScoreColor }">{{ hrLevelText }}</span>
                  <span class="hr-score-desc">候选人匹配度评估</span>
                  <span v-if="hrComment" class="hr-score-comment">{{ hrComment }}</span>
                </div>
              </div>
              <el-progress
                :percentage="hrScore"
                :stroke-width="12"
                :color="aiScoreColor"
                :show-text="false"
                class="hr-score-bar"
              />
            </div>
            <div v-if="hrDims && hrDims.length" class="ai-details">
              <div class="ai-subtitle">评分维度</div>
              <div v-for="(dim, i) in hrDims" :key="i" class="dim-row">
                <div class="dim-label">{{ dim.label || dim.name }}</div>
                <el-progress
                  :percentage="dim.percent || dim.score"
                  :stroke-width="8"
                  :color="hrDimColor(dim.percent || dim.score)"
                  :show-text="false"
                />
                <span class="dim-val">{{ dim.score || dim.percent }}</span>
              </div>
            </div>
          </template>

          <!-- HR 模式（空数据） -->
          <div v-else-if="useHrAI && (hrScore === null || hrScore === undefined)" class="ai-empty-state">
            <div class="ai-score-null" style="margin-bottom:8px">--</div>
            <p style="margin:0;font-size:13px;color:#C9CDD4">暂无匹配数据</p>
          </div>

          <!-- 空状态 -->
          <div v-if="!useTeacherAI && !useHrAI" class="ai-empty-state">
            <div class="ai-score-null" style="margin-bottom:8px">--</div>
            <p style="margin:0;font-size:13px;color:#C9CDD4">
              {{ mode === 'teacher' ? '还没有 AI 简历分析' : '暂无评分数据' }}
            </p>
          </div>

          <!-- 教师模式：AI 诊断操作（简历未变更时后端直接返回缓存结果，不重复消耗 AI） -->
          <div v-if="mode === 'teacher'" class="ai-run-area">
            <el-button type="primary" :loading="analyzing" @click="runAnalysis(false)">
              {{ aiAnalysisData ? '重新诊断' : 'AI 诊断' }}
            </el-button>
            <el-button v-if="aiAnalysisData" link size="small" class="ai-force-link" @click="forceAnalysis">
              简历刚改过？强制重新分析
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { resumeAPI, aiParseAPI } from '@/api'

const props = defineProps({
  visible: Boolean,
  studentId: { type: [String, Number], default: '' },
  className: { type: String, default: '' },
  // 模式：teacher → AI简历建议（aiAnalysis），hr → 岗位匹配度评分
  mode: {
    type: String,
    default: 'teacher',
    validator: (v) => ['teacher', 'hr'].includes(v)
  },
  // HR 模式：岗位匹配度评分
  hrScore: { type: Number, default: null },
  hrLevel: { type: String, default: '' },
  hrComment: { type: String, default: '' },
  hrDims: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:visible'])

const localVisible = ref(false)
const selectedStudent = ref(null)

watch(() => props.visible, (v) => {
  localVisible.value = v
  if (v && props.studentId) loadResume()
})
watch(localVisible, (v) => {
  if (!v) emit('update:visible', false)
})

const loadResume = async () => {
  selectedStudent.value = null
  try {
    const res = await resumeAPI.getResumeByStudent(props.studentId)
    if (res.code === 200 && res.data) {
      selectedStudent.value = res.data
    }
  } catch (e) { /* no resume */ }
}

// ---- 简历数据解析 ----
const parsedResume = computed(() => selectedStudent.value || {})

const resumeName = computed(() => {
  return parsedResume.value.realName || parsedResume.value.name || '学生'
})
const resumePhone = computed(() => parsedResume.value.phone || '--')
const resumeEmail = computed(() => parsedResume.value.email || '--')
const resumeMajor = computed(() => {
  const edu = parsedEducation.value
  return edu[0]?.major || parsedResume.value.major || '未设置'
})
const resumeSchool = computed(() => {
  const edu = parsedEducation.value
  return edu[0]?.school || parsedResume.value.school || ''
})
const resumeTags = computed(() => {
  const tags = []
  const firstEdu = parsedEducation.value[0]
  if (firstEdu?.degree) tags.push(firstEdu.degree)
  if (firstEdu?.endDate) tags.push(firstEdu.endDate.substring(0, 4) + '届')
  if (props.className) tags.push(props.className)
  return tags
})
const parsedEducation = computed(() => {
  const raw = parsedResume.value.education
  if (!raw) return []
  try {
    return Array.isArray(raw) ? raw : JSON.parse(raw)
  } catch {
    return []
  }
})
const parsedInternship = computed(() => {
  const raw = parsedResume.value.internship
  if (!raw) return []
  try {
    return Array.isArray(raw) ? raw : JSON.parse(raw)
  } catch { return [] }
})
const parsedProject = computed(() => {
  const raw = parsedResume.value.project
  if (!raw) return []
  try {
    return Array.isArray(raw) ? raw : JSON.parse(raw)
  } catch { return [] }
})
const parsedSkills = computed(() => {
  const raw = parsedResume.value.skills
  if (!raw) return []
  try { return JSON.parse(raw) } catch {
    return raw.split(/[,，、\n]/).map(s => s.trim()).filter(Boolean)
  }
})
const aiAnalysisData = computed(() => {
  const raw = parsedResume.value.aiAnalysis
  if (!raw) return null
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    return parsed && typeof parsed === 'object' ? parsed : null
  } catch { return null }
})

const useTeacherAI = computed(() => props.mode === 'teacher' && aiAnalysisData.value)
const useHrAI = computed(() => props.mode === 'hr')

const aiScoreColor = computed(() => {
  const score = useHrAI.value ? (props.hrScore ?? 0) : (aiAnalysisData.value?.overallScore || 0)
  if (score >= 80) return '#10B981'
  if (score >= 60) return '#F59E0B'
  return '#EF4444'
})

const hrLevelText = computed(() => {
  if (props.hrLevel) return props.hrLevel
  const s = props.hrScore
  if (s === null || s === undefined) return '暂无评分'
  if (s >= 85) return '非常匹配'
  if (s >= 70) return '比较匹配'
  if (s >= 60) return '一般匹配'
  return '匹配度较低'
})

const hrDimColor = (val) => {
  if (val >= 80) return '#10B981'
  if (val >= 60) return '#F59E0B'
  return '#EF4444'
}

// ---- AI 简历诊断（教师模式） ----
const analyzing = ref(false)

const runAnalysis = async (force) => {
  if (analyzing.value || !props.studentId) return
  analyzing.value = true
  try {
    const res = await aiParseAPI.analyzeResume(props.studentId, force)
    if (res.code === 200) {
      ElMessage.success(res.message || '简历分析成功')
      // 后端已把结果写回 aiAnalysis，重新拉取刷新侧栏
      await loadResume()
    } else {
      ElMessage.error(res.message || '简历分析失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '简历分析失败')
  } finally {
    analyzing.value = false
  }
}

const forceAnalysis = () => {
  ElMessageBox.confirm('将忽略缓存结果，重新调用 AI 分析该学生的简历。', '强制重新分析', {
    confirmButtonText: '重新分析',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => runAnalysis(true)).catch(() => {})
}
</script>

<style scoped>
/* ========== 双栏布局 ========== */
.resume-detail {
  display: flex;
  flex: 1;
  min-height: 0;
}
.resume-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.resume-ai-side {
  width: 320px;
  flex-shrink: 0;
  border-left: 1px solid #F2F3F5;
  background: #F9FAFB;
  overflow-y: auto;
  padding: 20px;
}
.ai-sticky {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 头部卡片 */
.info-header-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.06);
}
.basic-info { flex: 1; min-width: 0; }
.basic-info .name {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 700;
  color: #1D2129;
}
.basic-info .desc {
  margin: 0 0 8px;
  font-size: 13px;
  color: #86909C;
}
.contact-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.contact-row span {
  font-size: 12px;
  color: #86909C;
}
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tag-item {
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  background: #F2F3F5;
  color: #4E5969;
}

/* 内容卡片 */
.section-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 22px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.06);
}
.section-title {
  font-size: 15px;
  font-weight: 700;
  color: #1D2129;
  margin-bottom: 12px;
}
.section-text {
  margin: 0;
  font-size: 14px;
  color: #4E5969;
  line-height: 1.7;
  white-space: pre-wrap;
}
.empty-text {
  margin: 0;
  font-size: 13px;
  color: #C9CDD4;
}

/* 经历列表 */
.exp-list { display: flex; flex-direction: column; }
.exp-item {
  padding: 12px 0;
  border-bottom: 1px solid #F2F3F5;
}
.exp-item:last-child { border-bottom: none; padding-bottom: 0; }
.exp-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.exp-name {
  font-size: 14px;
  font-weight: 600;
  color: #1D2129;
}
.exp-time {
  font-size: 12px;
  color: #C9CDD4;
  flex-shrink: 0;
  margin-left: 12px;
}
.exp-sub {
  font-size: 13px;
  color: #4E5969;
  margin: 0;
}
.exp-desc {
  font-size: 13px;
  color: #86909C;
  margin: 4px 0 0;
  line-height: 1.5;
}

/* 技能标签 */
.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.skill-tag {
  padding: 5px 14px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;
  background: rgba(22,93,255,0.08);
  color: #165DFF;
}

/* AI 侧栏 */
.ai-score-card {
  text-align: center;
  background: #fff;
  border-radius: 12px;
  padding: 20px 16px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.06);
}
.ai-score-title {
  font-size: 14px;
  font-weight: 600;
  color: #1D2129;
  margin-bottom: 14px;
}
.ai-score-circle {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 2px;
}
.ai-score-num {
  font-size: 44px;
  font-weight: 800;
  line-height: 1;
}
.ai-score-total {
  font-size: 14px;
  color: #C9CDD4;
}
.ai-score-empty { padding: 16px 0; }
.ai-score-null {
  font-size: 36px;
  font-weight: 700;
  color: #E5E6EB;
  line-height: 1;
}
.ai-score-empty p {
  margin: 8px 0 0;
  font-size: 13px;
  color: #C9CDD4;
}
.ai-details {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.ai-subtitle {
  font-size: 13px;
  font-weight: 600;
  color: #4E5969;
  margin-bottom: 6px;
}
.ai-item {
  margin: 0 0 6px;
  font-size: 13px;
  padding: 8px 12px;
  border-radius: 8px;
  line-height: 1.5;
}
.ai-item:last-child { margin-bottom: 0; }
.ai-item--green { background: rgba(0,180,42,0.07); color: #00B42A; }
.ai-item--red { background: rgba(239,68,68,0.07); color: #EF4444; }
.ai-item--blue { background: rgba(22,93,255,0.07); color: #165DFF; }
.ai-item--amber { background: rgba(245,158,11,0.08); color: #D97706; }
.ai-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.ai-tag {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  background: rgba(139,92,246,0.1);
  color: #7C3AED;
}
.ai-empty-state { padding: 24px 0; }
.ai-run-area {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.ai-run-area .el-button {
  width: 100%;
}
.ai-force-link {
  align-self: center;
  font-size: 12px;
  color: #86909C;
}

/* HR 评分样式 */
.hr-score-hero {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.hr-score-circle {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(22,93,255,0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.hr-score-big {
  font-size: 26px;
  font-weight: 800;
  line-height: 1;
}
.hr-score-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  text-align: left;
}
.hr-score-level {
  font-size: 15px;
  font-weight: 700;
}
.hr-score-desc {
  font-size: 12px;
  color: #86909C;
}
.hr-score-comment {
  font-size: 12px;
  color: #4E5969;
  margin-top: 2px;
  line-height: 1.4;
}
.hr-score-bar {
  margin-top: 8px;
}
.dim-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.dim-row:last-child { margin-bottom: 0; }
.dim-label {
  font-size: 12px;
  color: #4E5969;
  width: 60px;
  flex-shrink: 0;
}
.dim-row .el-progress {
  flex: 1;
}
.dim-val {
  font-size: 12px;
  font-weight: 600;
  color: #4E5969;
  width: 24px;
  text-align: right;
  flex-shrink: 0;
}
</style>

<style>
/* 遮罩层覆盖全页 */
.resume-overlay {
  position: fixed !important;
  top: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  left: 0 !important;
  background: rgba(0, 0, 0, 0.45) !important;
  z-index: 9999 !important;
  overflow: hidden !important;
}
.resume-overlay .el-overlay-dialog {
  display: flex !important;
  justify-content: center;
  align-items: flex-start;
  padding-top: 4vh;
}
.resume-dialog {
  border-radius: 12px !important;
  overflow: hidden !important;
  display: flex !important;
  flex-direction: column !important;
  max-width: 1200px !important;
  max-height: 88vh !important;
}
.resume-dialog .el-dialog__header {
  padding: 16px 28px !important;
  border-bottom: 1px solid #F2F3F5;
  flex-shrink: 0 !important;
}
.resume-dialog .el-dialog__title {
  font-size: 16px !important;
  font-weight: 600 !important;
}
.resume-dialog .el-dialog__body {
  padding: 0 !important;
  overflow: hidden !important;
  flex: 1 !important;
  display: flex !important;
}
</style>
