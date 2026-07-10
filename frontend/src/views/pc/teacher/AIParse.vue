<template>
  <div class="ai-parse fade-in">
    <div class="page-header">
      <h2>AI 简历分析</h2>
      <p>智能分析学生简历质量并给出改进建议</p>
    </div>

    <div class="content-card">
      <div class="content-card-header">
        <span class="content-card-title">选择学生</span>
      </div>
      <el-form :inline="true" class="parse-form">
        <el-form-item label="选择班级">
          <el-select v-model="selectedClassId" placeholder="请选择班级" clearable style="width:200px" @change="onClassChange">
            <el-option v-for="c in classList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择学生">
          <el-select v-model="selectedStudentId" placeholder="请选择学生" filterable clearable style="width:260px" :disabled="!selectedClassId">
            <el-option v-for="s in studentList" :key="s.id" :label="(s.realName || s.username) + (s.username ? ' (' + s.username + ')' : '')" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="doAnalyze" :loading="analyzing" :disabled="!selectedStudentId">
            AI 分析
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="result" class="analyze-result">
      <!-- 综合评分 -->
      <div class="result-card score-card">
        <div class="score-circle" :style="{ borderColor: scoreColor(result.overallScore) }">
          <span class="score-value" :style="{ color: scoreColor(result.overallScore) }">{{ result.overallScore }}</span>
        </div>
        <div class="score-info">
          <h3>综合评分</h3>
          <p v-if="result.overallScore >= 80">简历质量优秀，整体结构完整</p>
          <p v-else-if="result.overallScore >= 60">简历质量良好，有改进空间</p>
          <p v-else>简历有待完善，建议参考改进建议</p>
        </div>
      </div>

      <div class="result-grid">
        <!-- 优势 -->
        <div class="result-card">
          <div class="result-card-header">
            <span class="result-icon icon-strength"></span>
            <h3>优势</h3>
          </div>
          <div v-if="result.strengths && result.strengths.length > 0">
            <el-tag v-for="(s, i) in result.strengths" :key="i" class="result-tag tag-strength" size="small">{{ s }}</el-tag>
          </div>
          <p v-else class="no-data">暂无数据</p>
        </div>

        <!-- 不足 -->
        <div class="result-card">
          <div class="result-card-header">
            <span class="result-icon icon-weakness"></span>
            <h3>不足</h3>
          </div>
          <div v-if="result.weaknesses && result.weaknesses.length > 0">
            <el-tag v-for="(w, i) in result.weaknesses" :key="i" class="result-tag tag-weakness" size="small">{{ w }}</el-tag>
          </div>
          <p v-else class="no-data">暂无数据</p>
        </div>
      </div>

      <!-- 改进建议 -->
      <div class="result-card">
        <div class="result-card-header">
          <span class="result-icon icon-suggestion"></span>
          <h3>改进建议</h3>
        </div>
        <ul v-if="result.suggestions && result.suggestions.length > 0" class="suggestion-list">
          <li v-for="(s, i) in result.suggestions" :key="i" class="suggestion-item">
            <span class="suggestion-num">{{ i + 1 }}</span>
            {{ s }}
          </li>
        </ul>
        <p v-else class="no-data">暂无数据</p>
      </div>

      <!-- 缺失字段 + 推荐技能 -->
      <div class="result-grid">
        <div class="result-card">
          <div class="result-card-header">
            <span class="result-icon icon-missing"></span>
            <h3>缺失字段</h3>
          </div>
          <div v-if="result.missingFields && result.missingFields.length > 0">
            <el-tag v-for="(f, i) in result.missingFields" :key="i" class="result-tag tag-missing" size="small">{{ f }}</el-tag>
          </div>
          <p v-else class="no-data">暂无缺失</p>
        </div>

        <div class="result-card">
          <div class="result-card-header">
            <span class="result-icon icon-skill"></span>
            <h3>推荐补充技能</h3>
          </div>
          <div v-if="result.recommendedSkills && result.recommendedSkills.length > 0">
            <el-tag v-for="(sk, i) in result.recommendedSkills" :key="i" class="result-tag tag-skill" size="small">{{ sk }}</el-tag>
          </div>
          <p v-else class="no-data">暂无数据</p>
        </div>
      </div>
    </div>

    <div v-if="!result && !analyzing && selectedStudentId" class="empty-state">
      <el-empty :image-size="100" description="点击「AI 分析」查看简历分析结果" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { classAPI, aiParseAPI } from '@/api'

const classList = ref([])
const studentList = ref([])
const selectedClassId = ref('')
const selectedStudentId = ref('')
const analyzing = ref(false)
const result = ref(null)

onMounted(async () => {
  await loadClasses()
})

const loadClasses = async () => {
  try {
    const res = await classAPI.getClasses({ page: 1, size: 200 })
    if (res.code === 200) {
      classList.value = res.data?.records || res.data || []
    }
  } catch (e) { console.error('加载班级失败', e) }
}

const onClassChange = async () => {
  selectedStudentId.value = ''
  result.value = null
  studentList.value = []
  if (!selectedClassId.value) return
  try {
    const res = await classAPI.getClassStudents(selectedClassId.value)
    if (res.code === 200) {
      const raw = res.data?.records || res.data || []
      studentList.value = Array.isArray(raw) ? raw : []
    }
  } catch (e) { console.error('加载学生列表失败', e) }
}

const doAnalyze = async () => {
  if (!selectedStudentId.value) { ElMessage.warning('请先选择学生'); return }
  analyzing.value = true
  result.value = null
  try {
    const res = await aiParseAPI.analyzeResume(selectedStudentId.value)
    if (res.code === 200) {
      result.value = res.data || {}
      ElMessage.success('分析完成')
    } else {
      ElMessage.error(res.message || '分析失败')
    }
  } catch (e) {
    ElMessage.error('分析失败: ' + (e.message || '该学生可能暂无简历'))
  } finally {
    analyzing.value = false
  }
}

const scoreColor = (score) => {
  if (!score) return '#C9CDD4'
  if (score >= 80) return '#10B981'
  if (score >= 60) return '#165DFF'
  if (score >= 40) return '#F59E0B'
  return '#EF4444'
}
</script>

<style scoped>
.parse-form { padding: 12px 0; }
.content-card { background: #fff; border-radius: 16px; padding: 24px; margin-bottom: 24px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); }
.content-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.content-card-title { font-size: 16px; font-weight: 600; color: #1D2129; }
.empty-state { text-align: center; padding: 60px 0; }

.analyze-result { display: flex; flex-direction: column; gap: 20px; }

/* 评分卡片 */
.score-card { display: flex; align-items: center; gap: 24px; background: #fff; border-radius: 16px; padding: 32px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); }
.score-circle { width: 100px; height: 100px; border-radius: 50%; border: 6px solid; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.score-value { font-size: 36px; font-weight: 700; }
.score-info h3 { margin: 0 0 4px; font-size: 18px; color: #1D2129; }
.score-info p { margin: 0; font-size: 14px; color: #86909C; }

/* 双栏网格 */
.result-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }

/* 结果卡片 */
.result-card { background: #fff; border-radius: 16px; padding: 24px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); }
.result-card-header { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; }
.result-card-header h3 { margin: 0; font-size: 15px; font-weight: 600; color: #1D2129; }

/* 图标 */
.result-icon { display: inline-block; width: 18px; height: 18px; border-radius: 4px; flex-shrink: 0; }
.icon-strength { background: #10B981; }
.icon-weakness { background: #EF4444; }
.icon-suggestion { background: #165DFF; }
.icon-missing { background: #F59E0B; }
.icon-skill { background: #7F77DD; }

/* 标签 */
.result-tag { margin: 0 6px 8px 0; }
.tag-strength { --el-tag-bg-color: #EAF3DE; --el-tag-text-color: #3B6D11; --el-tag-border-color: #C0DD97; }
.tag-weakness { --el-tag-bg-color: #FCEBEB; --el-tag-text-color: #A32D2D; --el-tag-border-color: #F7C1C1; }
.tag-missing { --el-tag-bg-color: #FAEEDA; --el-tag-text-color: #854F0B; --el-tag-border-color: #FAC775; }
.tag-skill { --el-tag-bg-color: #EEEDFE; --el-tag-text-color: #534AB7; --el-tag-border-color: #CECBF6; }

/* 建议列表 */
.suggestion-list { list-style: none; margin: 0; padding: 0; }
.suggestion-item { display: flex; align-items: flex-start; gap: 10px; padding: 8px 0; font-size: 14px; color: #4E5969; line-height: 1.6; border-bottom: 1px solid #F7F8FA; }
.suggestion-item:last-child { border-bottom: none; }
.suggestion-num { display: inline-flex; align-items: center; justify-content: center; width: 22px; height: 22px; border-radius: 50%; background: #E6F1FB; color: #185FA5; font-size: 12px; font-weight: 600; flex-shrink: 0; margin-top: 2px; }

.no-data { font-size: 13px; color: #C9CDD4; margin: 0; }
</style>
