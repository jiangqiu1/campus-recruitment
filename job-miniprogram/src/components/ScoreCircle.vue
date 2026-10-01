<template>
	<view class="score-circle" :style="{ width: size + 'px', height: size + 'px' }">
		<canvas canvas-id="scoreCanvas" class="canvas-bg" :style="{ width: size + 'px', height: size + 'px' }"></canvas>
		<view class="score-text">
			<text class="score-value">{{ score }}</text>
			<text class="score-unit">分</text>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted, watch, getCurrentInstance } from 'vue'

const props = defineProps({
	score: { type: Number, default: 0 },
	size: { type: Number, default: 72 },
	strokeWidth: { type: Number, default: 6 }
})

const instance = getCurrentInstance()

const drawCircle = () => {
	const ctx = uni.createCanvasContext('scoreCanvas', instance.proxy)
	const w = props.size
	const h = props.size
	const cx = w / 2
	const cy = h / 2
	const r = (w - props.strokeWidth) / 2

	ctx.beginPath()
	ctx.arc(cx, cy, r, 0, Math.PI * 2)
	ctx.setStrokeStyle('#F2F3F5')
	ctx.setLineWidth(props.strokeWidth)
	ctx.stroke()

	const val = Math.min(Math.max(props.score, 0), 100)
	const colors = val < 60 ? '#E34D4F' : val < 80 ? '#E38330' : '#165DFF'
	const endAngle = (val / 100) * Math.PI * 2 - Math.PI / 2
	ctx.beginPath()
	ctx.arc(cx, cy, r, -Math.PI / 2, endAngle)
	ctx.setStrokeStyle(colors)
	ctx.setLineWidth(props.strokeWidth)
	ctx.setLineCap('round')
	ctx.stroke()

	ctx.draw()
}

onMounted(() => drawCircle())
watch(() => props.score, () => drawCircle())
</script>

<style scoped lang="scss">
.score-circle {
	position: relative;
	align-items: center;
	justify-content: center;
}
.canvas-bg {
	position: absolute;
	top: 0;
	left: 0;
}
.score-text {
	align-items: center;
	justify-content: center;
	gap: 0;
}
.score-value {
	font-size: 18px;
	font-weight: 700;
	color: #18181A;
	line-height: 1;
}
.score-unit {
	font-size: 10px;
	color: #949599;
	line-height: 1;
	margin-top: 2px;
}
</style>
