# 批量生成缺失的 Vue 视图文件
# 用法：在 PowerShell 中运行此脚本

$basePath = "E:\校企项目\frontend\src\views"

# 定义需要创建的视图文件和它们的路由路径
$files = @(
  # 管理员端
  "admin\Dashboard.vue",
  "admin\UserManage.vue",
  "admin\CompanyManage.vue",
  "admin\ClassManage.vue",
  "admin\EnterpriseAudit.vue",
  "admin\OperationLog.vue",
  
  # 普通教师端
  "simple-teacher\ClassManagement.vue",
  "simple-teacher\ResumeManagement.vue",
  "simple-teacher\JobPosting.vue",
  "simple-teacher\DeliveryBoard.vue",
  "simple-teacher\CompanyResource.vue",
  "simple-teacher\MessageNotification.vue",
  "simple-teacher\PersonalSettings.vue",
  
  # 教师端（完整版）
  "teacher\Dashboard.vue",
  "teacher\ResumeManage.vue",
  "teacher\AIParse.vue",
  "teacher\JobMatch.vue",
  
  # HR端
  "hr\Dashboard.vue",
  "hr\CompanyProfile.vue",
  "hr\JobManage.vue",
  "hr\HRResumeManage.vue",
  "hr\ResumeScore.vue",
  "hr\DataStats.vue",
  "hr\JobAnalysis.vue",
  "hr\AccountManagement.vue"
)

# 生成简单的占位符内容
function Generate-VueContent($fileName) {
  $name = [System.IO.Path]::GetFileNameWithoutExtension($fileName)
  $title = $name -replace '([a-z])([A-Z])', '$1 $2' -replace '-', ' '
  
  return @"
<template>
  <div class="$name.toLowerCase()">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>$title</span>
        </div>
      </template>
      
      <div class="placeholder-content">
        <el-empty description="$title 页面正在开发中..." />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)

onMounted(() => {
  console.log('$title 页面加载完成')
})

// 这里添加页面逻辑
</script>

<style scoped>
.$name.toLowerCase() {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.placeholder-content {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
"@
}

# 创建目录并写入文件
$createdCount = 0
$skippedCount = 0

foreach ($file in $files) {
  $fullPath = Join-Path $basePath $file
  $directory = [System.IO.Path]::GetDirectoryName($fullPath)
  
  # 创建目录（如果不存在）
  if (-not (Test-Path $directory)) {
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
    Write-Host "✅ 创建目录: $directory" -ForegroundColor Green
  }
  
  # 检查文件是否已存在
  if (Test-Path $fullPath) {
    Write-Host "⏭️  跳过已存在的文件: $file" -ForegroundColor Yellow
    $skippedCount++
  } else {
    # 生成并写入内容
    $content = Generate-VueContent $file
    $content | Out-File -FilePath $fullPath -Encoding UTF8
    Write-Host "✅ 创建文件: $file" -ForegroundColor Cyan
    $createdCount++
  }
}

Write-Host "`n==========================================" -ForegroundColor Green
Write-Host "批量创建完成！" -ForegroundColor Green
Write-Host "创建: $createdCount 个文件" -ForegroundColor Cyan
Write-Host "跳过: $skippedCount 个文件（已存在）" -ForegroundColor Yellow
Write-Host "==========================================`n" -ForegroundColor Green

# 列出所有已创建的视图文件
Write-Host "已创建的视图文件：" -ForegroundColor Green
Get-ChildItem -Path $basePath -Recurse -Filter "*.vue" | ForEach-Object {
  $relativePath = $_.FullName.Replace($basePath, "").TrimStart("\")
  Write-Host "  - $relativePath" -ForegroundColor Gray
}
