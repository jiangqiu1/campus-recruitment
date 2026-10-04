<template>
	<view class="page-wrapper">
		<NavBar title="AI 简历解析" show-back :right-text="showHistory ? '解析' : '历史'" @rightClick="toggleMode" />
		<scroll-view class="content-scrollable" scroll-y>
			<!-- 解析面板 -->
			<view v-if="!showHistory" class="parse-panel">
				<view class="panel-header">
					<uni-icons type="file" size="28" color="#0EA5E9" />
					<text class="panel-title">AI 简历解析</text>
				</view>
				<text class="panel-desc">粘贴学生简历原始文本，AI自动提取关键信息</text>

				<view class="input-section">
					<text class="input-label">原始文本</text>
					<textarea v-model="rawText" class="raw-input" placeholder="粘贴简历文本..." :maxlength="5000" />
					<text class="input-count">{{ rawText.length }}/5000</text>
				</view>

				<button class="parse-btn" :loading="parsing" @click="doParse" :disabled="!rawText.trim()">
					{{ parsing ? '解析中...' : 'AI 解析' }}
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
					<uni-icons type="list" size="28" color="#0EA5E9" />
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
								{{ log.isManualCorrected == 1 ? '已修正' : '待修正' }}
							</text>
						</view>
					</view>
				</view>
				<EmptyState v-else icon="file" title="暂无解析记录" desc="使用AI解析功能后记录会出现在这里" />
			</view>
		</scroll-view>

		<!-- 修正弹窗 -->
		<PopupDrawer :show="showCorrection" title="人工修正" @update:show="showCorrection = $event">
			<textarea v-model="correctedText" class="correct-input" placeholder="粘贴修正后的 JSON 文本..." />
			<view class="modal-actions">
				<button class="modal-btn cancel" @click="showCorrection = false">取消</button>
				<button class="modal-btn confirm" @click="submitCorrection">保存修正</button>
			</view>
		</PopupDrawer>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { aiParseAPI } from '@/utils/request'
import { getCurrentUser } from '@/utils/auth'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import PopupDrawer from '@/components/PopupDrawer.vue'
import { checkRole } from '@/utils/auth'

checkRole(1)

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

const toggleMode = () => { showHistory.value = !showHistory.value }

onMounted(async () => { await loadLogs() })

const loadLogs = async () => {
	try {
		const user = getCurrentUser()
		if (!user) return
		const res = await aiParseAPI.getLogs(user.id)
		logs.value = (res.data || []).sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
	} catch (e) { console.error('加载解析历史失败', e) }
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
		} catch (e) { parsed.value = { raw: parsedStr } }
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
	} catch (e) { uni.showToast({ title: '保存失败', icon: 'none' }) }
}

const formatTime = (t) => t ? t.substring(0, 16).replace('T', ' ') : ''
const formatPercent = (s) => s != null ? Math.round(Number(s) * 100) + '%' : '--'
</script>

<style scoped lang="scss">
.parse-panel, .history-panel { padding: 16px; }
.panel-header { flex-direction: row; align-items: center; gap: 10px; margin-bottom: 8px; }
.panel-title { font-size: 18px; font-weight: 700; color: $uni-text-color-title; }
.panel-desc { font-size: 13px; color: $uni-text-color-secondary; margin-bottom: 16px; }

.input-section { margin-bottom: 12px; }
.input-label { font-size: 14px; font-weight: 600; color: $uni-text-color-title; margin-bottom: 8px; display: block; }
.raw-input {
	width: 100%;
	height: 200px;
	background: $uni-bg-color-page;
	border: 1px solid $uni-border-color;
	border-radius: 8px;
	padding: 12px;
	font-size: 14px;
	line-height: 1.6;
}
.raw-input:focus { border-color: $uni-color-primary; background: $uni-bg-color; }
.input-count { font-size: 12px; color: $uni-text-color-placeholder; text-align: right; margin-top: 4px; display: block; }

.parse-btn {
	width: 100%;
	height: 46px;
	border-radius: 8px;
	background: $uni-gradient-primary;
	color: $uni-text-color-inverse;
	font-size: 16px;
	font-weight: 700;
	border: none;
	align-items: center;
	justify-content: center;
}
.parse-btn[disabled] { background: $uni-border-color !important; color: $uni-text-color-placeholder !important; }

.result-section {
	margin-top: 20px;
	background: $uni-color-primary-light;
	border-radius: 12px;
	padding: 16px;
}
.result-header { flex-direction: row; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.result-title { font-size: 16px; font-weight: 700; color: $uni-text-color-title; }
.confidence-badge { padding: 3px 10px; background: $uni-color-primary-light; border-radius: 12px; font-size: 12px; color: $uni-color-primary-hover; }
.result-content { flex-direction: column; gap: 8px; }
.result-item { flex-direction: row; padding: 8px 12px; background: $uni-bg-color; border-radius: 8px; }
.result-key { width: 80px; font-size: 13px; font-weight: 600; color: $uni-text-color; flex-shrink: 0; }
.result-val { flex: 1; font-size: 13px; color: $uni-text-color-title; }

.log-list { flex-direction: column; gap: 10px; }
.log-card { background: $uni-bg-color; border-radius: 12px; padding: 14px; box-shadow: $uni-shadow-card; }
.log-top { flex-direction: row; justify-content: space-between; margin-bottom: 6px; }
.log-time { font-size: 12px; color: $uni-text-color-secondary; }
.log-confidence { font-size: 12px; font-weight: 600; color: $uni-color-primary; }
.log-preview { font-size: 13px; color: $uni-text-color; display: block; margin-bottom: 6px; }
.log-status { font-size: 12px; padding: 2px 8px; border-radius: 4px; background: $uni-color-warning-light; color: $uni-color-warning; }
.log-status.corrected { background: $uni-color-primary-light; color: $uni-color-primary; }

.correct-input {
	width: 100%;
	height: 150px;
	border: 1px solid $uni-border-color;
	border-radius: 8px;
	padding: 10px;
	font-size: 13px;
	margin-bottom: 16px;
	background: $uni-bg-color-page;
}
.modal-actions { flex-direction: row; gap: 12px; }
.modal-btn {
	flex: 1;
	height: 42px;
	border-radius: 8px;
	align-items: center;
	justify-content: center;
	font-size: 15px;
	font-weight: 600;
	border: none;
}
.modal-btn.cancel { background: $uni-bg-color-page; color: $uni-text-color; }
.modal-btn.confirm { background: $uni-color-primary; color: $uni-text-color-inverse; }
</style>
