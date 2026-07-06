<template>
	<view class="job-card" @click="handleClick">
		<view class="card-main">
			<view class="card-left">
				<view class="title-row">
					<text class="job-title">{{ job.title }}</text>
					<view class="title-right">
						<view v-if="showMatch && matchScore" class="match-badge" :style="{ background: matchBadgeBg }">
							<text class="match-badge-text">{{ matchScore }}%</text>
						</view>
						<text class="salary">{{ job.salaryText || job.salaryRange }}</text>
					</view>
				</view>
				<text class="company-name">{{ job.companyName }}</text>
				<view class="job-tags">
					<text class="base-tag">{{ job.location }}</text>
					<text class="base-tag">{{ job.education || job.educationLevel }}</text>
					<text v-if="job.experience || job.experienceLevel" class="base-tag">{{ job.experience || job.experienceLevel }}</text>
				</view>
				<view v-if="job.tags && job.tags.length" class="welfare-tags">
					<text v-for="(tag, i) in job.tags.slice(0, 3)" :key="i" class="welfare-tag">{{ tag }}</text>
				</view>
			</view>
		</view>
		<view v-if="showActions" class="card-actions">
			<button
				v-if="showDeliver"
				class="action-btn btn-primary"
				:class="{ 'btn-disabled': delivered }"
				:disabled="delivered"
				@click.stop="handleDeliver"
			>
				{{ delivered ? '已投递' : '立即投递' }}
			</button>
			<button
				v-if="showCollect"
				class="action-btn btn-outline"
				@click.stop="handleCollect"
			>
				<uni-icons :type="collected ? 'star-filled' : 'star'" size="14" :color="collected ? '#FF7D00' : '#86909C'" />
				<text>{{ collected ? '已收藏' : '收藏' }}</text>
			</button>
		</view>
	</view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
	job: { type: Object, default: () => ({}) },
	showDeliver: { type: Boolean, default: true },
	showCollect: { type: Boolean, default: false },
	showMatch: { type: Boolean, default: false },
	matchScore: { type: Number, default: 0 },
	delivered: { type: Boolean, default: false },
	collected: { type: Boolean, default: false },
	showActions: { type: Boolean, default: true }
})
const emit = defineEmits(['click', 'deliver', 'collect'])
const handleClick = () => emit('click', props.job)
const handleDeliver = () => { if (!props.delivered) emit('deliver', props.job) }
const handleCollect = () => emit('collect', props.job)

const matchBadgeBg = computed(() => {
	const s = props.matchScore
	if (s >= 85) return 'linear-gradient(135deg, #00B42A 0%, #7BE188 100%)'
	if (s >= 70) return 'linear-gradient(135deg, #165DFF 0%, #60A5FA 100%)'
	if (s >= 50) return 'linear-gradient(135deg, #FF7D00 0%, #FFC166 100%)'
	return 'linear-gradient(135deg, #86909C 0%, #C9CDD4 100%)'
})
</script>

<style scoped>
.job-card {
	background: #FFFFFF;
	border-radius: 12px;
	padding: 16px;
	margin-bottom: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
	transition: background 0.2s;
}
.job-card:active {
	background: #F7F8FA;
}
.card-main {
	width: 100%;
}
.card-left {
	width: 100%;
	gap: 8px;
}
.title-row {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
}
.job-title {
	font-size: 16px;
	font-weight: 600;
	color: #1D2129;
	flex: 1;
	margin-right: 8px;
}
.title-right {
	flex-direction: row;
	align-items: center;
	gap: 8px;
	flex-shrink: 0;
}
.match-badge {
	padding: 2px 8px;
	border-radius: 10px;
	align-items: center;
	justify-content: center;
}
.match-badge-text {
	font-size: 11px;
	font-weight: 700;
	color: #FFFFFF;
}
.salary {
	font-size: 16px;
	font-weight: 700;
	color: #165DFF;
	flex-shrink: 0;
}
.company-name {
	font-size: 13px;
	color: #4E5969;
}
.job-tags {
	flex-direction: row;
	flex-wrap: wrap;
	gap: 6px;
}
.base-tag {
	font-size: 12px;
	color: #86909C;
	background: #F2F3F5;
	padding: 2px 8px;
	border-radius: 4px;
}
.welfare-tags {
	flex-direction: row;
	flex-wrap: wrap;
	gap: 6px;
}
.welfare-tag {
	font-size: 12px;
	color: #165DFF;
	background: rgba(22, 93, 255, 0.08);
	padding: 2px 8px;
	border-radius: 4px;
}
.card-actions {
	flex-direction: row;
	gap: 12px;
	margin-top: 16px;
	padding-top: 16px;
	border-top: 1px solid #F7F8FA;
}
.action-btn {
	flex: 1;
	height: 36px;
	border-radius: 8px;
	border: none;
	font-size: 13px;
	font-weight: 500;
	align-items: center;
	justify-content: center;
	flex-direction: row;
	gap: 4px;
}
.btn-primary {
	background: #165DFF;
	color: #FFFFFF;
}
.btn-outline {
	background: #FFFFFF;
	border: 1px solid #E5E6EB;
	color: #4E5969;
}
.btn-disabled {
	background: #E5E6EB;
	color: #A9AEB8;
	border: none;
}
.btn-primary:active, .btn-outline:active {
	opacity: 0.85;
}
</style>
