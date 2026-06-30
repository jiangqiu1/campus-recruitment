<template>
	<view class="page-wrapper">
		<view class="header-bar">
			<view class="header-left" @click="goBack"><text class="back-arrow">←</text></view>
			<text class="header-title">AI 简历解析</text>
			<view class="header-right" @click="showHistory = !showHistory">
				<text class="history-btn">{{ showHistory ? '✏️ 解析' : '📋 历史' }}</text>
			</view>
		</view>

		<scroll-view class="content-scrollable" scroll-y>
			<!-- 解析面板 -->
			<view v-if="!showHistory" class="parse-panel">
				<view class="panel-header">
					<text class="panel-icon">📄</text>
					<text class="panel-title">AI 简历解析</text>
				</view>
				<text class="panel-desc">粘贴学生简历原始文本，AI自动提取关键信息</text>

				<view class="input-section">
					<text class="input-label">原始文本</text>
					<textarea v-model="rawText" class="raw-input" placeholder="粘贴简历文本..." :maxlength="5000" />
					<text class="input-count">{{ rawText.length }}/5000</text>
				</view>

				<button class="parse-btn" :loading="parsing" @click="doParse" :disabled="!rawText.trim()">
					{{ parsing ? '解析中...' : '🤖 AI 解析' }}
				</button>

				<!-- 解析结果 -->
				<view v-if="parseResult" class="result-section">
					<view class="result-header">
						<text class="result-title">解析结果</text>
						<view class="confidence-badge">
							<text>置信度 {{ formatPercent(confidenceScore) }}</text>
						</view>
					</view>
					<view class="result-content">
						<view v-for="(val, key) in parsed" :key="key" class="result-item">
							<text class="result-key">{{ keyLabels[key] || key }}</text>
							<text class="result-val">{{ typeof val === 'object' ? (val || []).join('、') : val }}</text>
						</view>
					</view>
				</view>
			</view>

			<!-- 解析历史 -->
			<view v-else class="history-panel">
				<view class="panel-header">
					<text class="panel-icon">📋</text>
					<text class="panel-title">解析历史</text>
				</view>

				<view v-if="logs.length > 0" class="log-list">
					<view v-for="(log, i) in logs" :key="i" class="log-card" @click="viewLogDetail(log)">
						<view class="log-top">
							<text class="log-time">{{ formatTime(log.createTime) }}</text>
							<text class="log-confidence">{{ formatPercent(log.confidenceScore) }}</text>
						</view>
						<text class="log-preview">{{ (log.rawMessage || '').substring(0, 60) }}...</text>
						<view class="log-meta">
							<text class="log-status" :class="{ corrected: log.isManualCorrected == 1 }">
								{{ log.isManualCorrected == 1 ? '✅ 已修正' : '⏳ 待修正' }}
							</text>
						</view>
					</view>
				</view>
				<view v-else class="empty-state">
					<text class="empty-icon">📄</text>
					<text class="empty-title">暂无解析记录</text>
				</view>
			</view>
		</scroll-view>

		<!-- 修正弹窗 -->
		<view v-if="showCorrection" class="modal-overlay" @click.self="showCorrection = false">
			<view class="modal-content">
				<text class="modal-title">人工修正</text>
				<textarea v-model="correctedText" class="correct-input" placeholder="粘贴修正后的 JSON 文本..." />
				<view class="modal-actions">
					<view class="modal-btn cancel" @click="showCorrection = false">取消</view>
					<view class="modal-btn confirm" @click="submitCorrection">保存修正</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { aiParseAPI } from '@/utils/request'
import { getCurrentUser } from '@/utils/auth'

const rawText = ref('')
const parsing = ref(false)
const parseResult = ref(null)
const parsed = ref({})
const confidenceScore = ref(0)
const logs = ref([])
const showHistory = ref(false)
const showCorrection = ref(false)
const correctedText = ref('')
const editingLogId = ref(null)

const keyLabels = {
  education: '学历',
  skills: '技能',
  experience: '工作经验',
  school: '毕业院校',
  major: '专业',
  name: '姓名',
  phone: '电话',
  email: '邮箱'
}

onMounted(async () => {
  await loadLogs()
})

const loadLogs = async () => {
  try {
    const user = getCurrentUser()
    if (!user) return
    const res = await aiParseAPI.getLogs(user.id)
    logs.value = (res.data || []).sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
  } catch (e) {
    console.error('加载解析历史失败', e)
  }
}

const doParse = async () => {
  if (!rawText.value.trim()) return
  parsing.value = true
  try {
    const user = getCurrentUser()
    const res = await aiParseAPI.parse({ teacherId: user.id, rawMessage: rawText.value })
    const result = res.data || {}
    const parsedStr = result.parsedResult || '{}'
    confidenceScore.value = parseFloat(result.confidenceScore || 0)

    try {
      parsed.value = JSON.parse(typeof parsedStr === 'string' ? parsedStr : JSON.stringify(parsedStr))
    } catch (e) {
      parsed.value = { raw: parsedStr }
    }
    parseResult.value = true

    await loadLogs()
  } catch (e) {
    console.error('解析失败', e)
    uni.showToast({ title: '解析失败', icon: 'none' })
  } finally {
    parsing.value = false
  }
}

const viewLogDetail = (log) => {
  showCorrection.value = true
  editingLogId.value = log.id
  correctedText.value = log.correctedResult || log.parsedResult || ''
}

const submitCorrection = async () => {
  if (!editingLogId.value || !correctedText.value.trim()) return
  try {
    await aiParseAPI.correct(editingLogId.value, correctedText.value)
    uni.showToast({ title: '修正已保存', icon: 'success' })
    showCorrection.value = false
    await loadLogs()
  } catch (e) {
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}

const formatTime = (t) => t ? t.substring(0, 16).replace('T', ' ') : ''
const formatPercent = (s) => s != null ? Math.round(Number(s) * 100) + '%' : '--'
const goBack = () => uni.navigateBack()
</script>

<style scoped>
.header-bar {
	display: flex;
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
	padding: 12px 16px;
	background: #fff;
	border-bottom: 1px solid #F0F2F5;
	position: sticky;
	top: 0; z-index: 10;
}
.header-left { width: 40px; }
.header-title { font-size: 17px; font-weight: 700; color: #1D2129; }
.history-btn { font-size: 14px; color: #10B981; font-weight: 600; padding: 4px 12px; border-radius: 6px; background: #F0FDF4; }

.parse-panel, .history-panel { padding: 16px; }
.panel-header {
	display: flex;
	flex-direction: row;
	align-items: center;
	gap: 10px;
	margin-bottom: 8px;
}
.panel-icon { font-size: 28px; }
.panel-title { font-size: 18px; font-weight: 700; color: #1D2129; }
.panel-desc { font-size: 13px; color: #86909C; margin-bottom: 16px; }

.input-section { margin-bottom: 12px; }
.input-label { font-size: 14px; font-weight: 600; color: #1D2129; margin-bottom: 8px; display: block; }
.raw-input {
	width: 100%;
	height: 200px;
	background: #F8F9FC;
	border: 2px solid #E2E8F0;
	border-radius: 12px;
	padding: 12px;
	font-size: 14px;
	line-height: 1.6;
}
.raw-input:focus { border-color: #10B981; background: #fff; }
.input-count { font-size: 11px; color: #C9CDD4; text-align: right; margin-top: 4px; display: block; }

.parse-btn {
	width: 100%;
	height: 46px;
	border-radius: 12px;
	background: linear-gradient(135deg, #10B981, #059669);
	color: #fff;
	font-size: 16px;
	font-weight: 700;
	border: none;
	align-items: center;
	justify-content: center;
	box-shadow: 0 4px 14px rgba(16,185,129,0.3);
}

.result-section {
	margin-top: 20px;
	background: #F0FDF4;
	border-radius: 14px;
	padding: 16px;
}
.result-header {
	display: flex;
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 12px;
}
.result-title { font-size: 16px; font-weight: 700; color: #065F46; }
.confidence-badge {
	padding: 3px 10px;
	background: #D1FAE5;
	border-radius: 12px;
	font-size: 12px;
	color: #059669;
}
.result-content { display: flex; flex-direction: column; gap: 8px; }
.result-item {
	display: flex;
	flex-direction: row;
	padding: 8px 12px;
	background: #fff;
	border-radius: 8px;
}
.result-key { width: 80px; font-size: 13px; font-weight: 600; color: #4E5969; flex-shrink: 0; }
.result-val { flex: 1; font-size: 13px; color: #1D2129; }

/* History */
.log-list { display: flex; flex-direction: column; gap: 10px; }
.log-card {
	background: #fff;
	border-radius: 12px;
	padding: 14px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.log-top {
	display: flex;
	flex-direction: row;
	justify-content: space-between;
	margin-bottom: 6px;
}
.log-time { font-size: 12px; color: #86909C; }
.log-confidence { font-size: 12px; font-weight: 600; color: #10B981; }
.log-preview { font-size: 13px; color: #4E5969; display: block; margin-bottom: 6px; }
.log-status { font-size: 11px; padding: 2px 8px; border-radius: 4px; background: #FFF7E6; color: #F59E0B; }
.log-status.corrected { background: #F0FDF4; color: #10B981; }

/* Modal */
.modal-overlay {
	position: fixed;
	top: 0; left: 0; right: 0; bottom: 0;
	background: rgba(0,0,0,0.4);
	align-items: center;
	justify-content: center;
	z-index: 999;
}
.modal-content {
	background: #fff;
	border-radius: 16px;
	padding: 24px;
	margin: 0 20px;
	width: calc(100% - 40px);
}
.modal-title { font-size: 17px; font-weight: 700; color: #1D2129; margin-bottom: 12px; }
.correct-input {
	width: 100%;
	height: 150px;
	border: 2px solid #E2E8F0;
	border-radius: 10px;
	padding: 10px;
	font-size: 13px;
	margin-bottom: 16px;
}
.modal-actions {
	display: flex;
	flex-direction: row;
	gap: 12px;
}
.modal-btn {
	flex: 1;
	height: 42px;
	border-radius: 10px;
	align-items: center;
	justify-content: center;
	font-size: 15px;
	font-weight: 600;
}
.modal-btn.cancel { background: #F5F7FA; color: #4E5969; }
.modal-btn.confirm { background: #10B981; color: #fff; }

.empty-state { align-items: center; padding: 40px; }
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty-title { font-size: 15px; color: #86909C; }
</style>
