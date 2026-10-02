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
				<view v-if="delivered" class="delivered-row">
					<text class="delivered-tag">已投递</text>
				</view>
				<view v-if="job.tags && job.tags.length" class="welfare-tags">
					<text v-for="(tag, i) in job.tags.slice(0, 3)" :key="i" class="welfare-tag">{{ tag }}</text>
				</view>
			</view>
		</view>
		<view v-if="showActions && (!delivered || showCollect)" class="card-actions">
			<button
				v-if="showDeliver && !delivered"
				class="action-btn btn-primary"
				@click.stop="handleDeliver"
			>
				立即投递
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

<style scoped lang="scss">
.job-card {
	position: relative;
	background: $uni-bg-color;
	border-radius: 12px;
	padding: 16px;
	margin-bottom: 12px;
	/* 列表卡降噪：浅边框替代阴影，避免大量"漂浮白块" */
	border: 1px solid $uni-border-color-divider;
	transition: background 0.2s;
}
.job-card:active {
	background: $uni-bg-color-page;
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
	color: $uni-text-color-title;
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
	color: $uni-text-color-inverse;
}
.salary {
	font-size: 16px;
	font-weight: 700;
	color: $uni-color-primary;
	flex-shrink: 0;
}
.company-name {
	font-size: 13px;
	color: $uni-text-color;
}
.job-tags {
	flex-direction: row;
	flex-wrap: wrap;
	gap: 6px;
}
.base-tag {
	font-size: 12px;
	color: $uni-text-color-secondary;
	background: $uni-border-color-divider;
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
	color: $uni-color-primary;
	background: $uni-color-primary-light;
	padding: 2px 8px;
	border-radius: 4px;
}
.delivered-row { margin-top: 8px; }
.delivered-tag {
	font-size: 12px;
	color: $uni-text-color-secondary;
	background: $uni-border-color-divider;
	padding: 2px 8px;
	border-radius: 4px;
}
.card-actions {
	flex-direction: row;
	gap: 12px;
	margin-top: 16px;
	padding-top: 16px;
	border-top: 1px solid $uni-bg-color-page;
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
	background: $uni-color-primary;
	color: $uni-text-color-inverse;
}
.btn-outline {
	background: $uni-bg-color;
	border: 1px solid $uni-border-color;
	color: $uni-text-color;
}
.btn-disabled {
	background: $uni-border-color;
	color: $uni-text-color-placeholder;
	border: none;
}
.btn-primary:active, .btn-outline:active {
	opacity: 0.85;
}
</style>
