<template>
  <el-breadcrumb class="breadcrumb" separator="/">
    <el-breadcrumb-item :to="{ path: '/' }">
      首页
    </el-breadcrumb-item>
    <el-breadcrumb-item
      v-for="(item, index) in breadcrumbList"
      :key="index"
      :to="item.path ? { path: item.path } : null"
    >
      {{ item.title }}
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const breadcrumbList = ref([])

// 生成面包屑
function generateBreadcrumb() {
  const matched = route.matched
  const list = []
  
  matched.forEach((item) => {
    if (item.meta && item.meta.title) {
      list.push({
        title: item.meta.title,
        path: item.path
      })
    }
  })
  
  breadcrumbList.value = list
}

// 监听路由变化
watch(
  () => route.path,
  () => generateBreadcrumb(),
  { immediate: true }
)
</script>

<style scoped>
.breadcrumb {
  margin-left: 15px;
  font-size: 14px;
}
</style>
