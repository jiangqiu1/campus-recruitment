<template>
  <div class="ai-insight">
    <!-- 初始态：引导条 -->
    <div v-if="!generated && !loading" class="insight-starter">
      <span class="starter-text">✦ 让 AI 解读本周就业数据，给出可执行建议</span>
      <button class="starter-btn" @click="generate">✦ AI 解读</button>
    </div>

    <!-- 加载态 -->
    <div v-else-if="loading" class="insight-body">
      <span class="insight-title">✦ AI 就业洞察</span>
      <div class="insight-loading">AI 正在解读数据，约需 5~20 秒…</div>
    </div>

    <!-- 结果态 -->
    <div v-else class="insight-body">
      <div class="insight-head">
        <span class="insight-title">✦ AI 就业洞察</span>
        <span class="provider-switch">
          <span class="p-chip" :class="{ active: provider === 'glm' }" @click="switchProvider('glm')">智谱 GLM</span>
          <span class="p-chip" :class="{ active: provider === 'deepseek' }" @click="switchProvider('deepseek')">DeepSeek</span>
        </span>
      </div>
      <template v-if="!isMock">
        <ul class="insight-list">
          <li v-for="(t, i) in insights" :key="i">{{ t }}</li>
        </ul>
        <div class="insight-suggestion" v-if="suggestion"><b>建议：</b>{{ suggestion }}</div>
        <div class="insight-foot">由 {{ providerLabel }}（{{ model }}）生成 · 基于真实统计数据 · <a @click="generate">重新生成</a></div>
      </template>
      <div v-else class="insight-loading">AI 服务暂不可用，请稍后重试</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { statisticsAPI } from '@/api'

const provider = ref('glm')
const data = ref(null)
const loading = ref(false)
const generated = ref(false)

const insights = computed(() => data.value?.insights || [])
const suggestion = computed(() => data.value?.suggestion || '')
const isMock = computed(() => !!data.value?.mock)
const model = computed(() => data.value?.model || '')
const providerLabel = computed(() => provider.value === 'glm' ? '智谱 GLM' : 'DeepSeek')

const generate = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await statisticsAPI.getAIInsight(provider.value)
    if (res.code === 200) {
      data.value = res.data || {}
      generated.value = true
    }
  } catch (e) {
    console.error('AI 洞察生成失败', e)
  } finally {
    loading.value = false
  }
}

const switchProvider = (p) => {
  if (p === provider.value) return
  provider.value = p
  generate()
}
</script>

<style scoped>
.ai-insight {
  background: linear-gradient(135deg, rgba(14,165,233,.06), rgba(22,93,255,.04));
  border: 1px solid rgba(14,165,233,.25);
  border-radius: 14px;
  margin-bottom: 20px;
  overflow: hidden;
}
.insight-starter {
  display: flex; align-items: center; justify-content: space-between; padding: 16px 20px;
}
.starter-text { color: #0284C7; font-size: 13.5px; font-weight: 500; }
.starter-btn {
  height: 34px; padding: 0 16px; border: none; border-radius: 10px; cursor: pointer;
  background: linear-gradient(135deg, #0EA5E9, #0284C7); color: #fff; font-size: 13px; font-weight: 600;
}
.insight-body { padding: 16px 20px; }
.insight-title { font-size: 15px; font-weight: 700; color: #0284C7; }
.insight-loading { margin-top: 10px; color: #86909C; font-size: 13.5px; animation: ai-pulse 1.2s ease-in-out infinite; }
@keyframes ai-pulse { 0%, 100% { opacity: 1; } 50% { opacity: .45; } }
.insight-head { display: flex; align-items: center; justify-content: space-between; }
.provider-switch { display: flex; gap: 6px; }
.p-chip {
  font-size: 12px; padding: 3px 10px; border-radius: 999px; cursor: pointer;
  color: #86909C; background: rgba(255,255,255,.7);
}
.p-chip.active { color: #fff; background: #0EA5E9; font-weight: 600; }
.insight-list { margin: 12px 0 0; padding-left: 0; list-style: none; }
.insight-list li {
  position: relative; padding-left: 18px; margin-bottom: 8px;
  font-size: 13.5px; color: #4E5969; line-height: 1.65;
}
.insight-list li::before { content: '✦'; position: absolute; left: 0; color: #0EA5E9; }
.insight-suggestion {
  margin-top: 10px; padding: 10px 14px; border-radius: 10px;
  background: rgba(255,255,255,.75); font-size: 13.5px; color: #4E5969; line-height: 1.65;
}
.insight-foot { margin-top: 12px; font-size: 12px; color: #C9CDD4; }
.insight-foot a { color: #0284C7; cursor: pointer; }
</style>
