<template>
	<view class="tag-input">
		<view class="tag-list">
			<view v-for="(tag, i) in modelValue" :key="i" class="tag-item">
				<text class="tag-text">{{ tag }}</text>
				<text class="tag-remove" @click="handleRemove(i)">✕</text>
			</view>
			<input
				class="tag-input-field"
				v-model="inputVal"
				:placeholder="placeholder"
				@confirm="handleAdd"
				@blur="handleBlur"
			/>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
	modelValue: { type: Array, default: () => [] },
	placeholder: { type: String, default: '输入后按回车添加' }
})

const emit = defineEmits(['update:modelValue'])
const inputVal = ref('')

const handleAdd = () => {
	const val = inputVal.value.trim()
	if (val && !props.modelValue.includes(val)) {
		emit('update:modelValue', [...props.modelValue, val])
	}
	inputVal.value = ''
}

const handleBlur = () => {
	if (inputVal.value.trim()) handleAdd()
}

const handleRemove = (index) => {
	const next = [...props.modelValue]
	next.splice(index, 1)
	emit('update:modelValue', next)
}
</script>

<style scoped>
.tag-input {
	border: 1px solid #E8EAED;
	border-radius: 8px;
	padding: 8px 12px;
	background: #F8F9FC;
}
.tag-list {
	flex-direction: row;
	flex-wrap: wrap;
	gap: 8px;
	align-items: center;
}
.tag-item {
	flex-direction: row;
	align-items: center;
	background: rgba(22, 93, 255, 0.08);
	border-radius: 4px;
	padding: 4px 8px;
	gap: 4px;
}
.tag-text {
	font-size: 13px;
	color: #165DFF;
}
.tag-remove {
	font-size: 12px;
	color: #949599;
	padding: 0 2px;
}
.tag-input-field {
	flex: 1;
	min-width: 80px;
	height: 28px;
	font-size: 14px;
	color: #303133;
	background: transparent;
	border: none;
	outline: none;
}
</style>
