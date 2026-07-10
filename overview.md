# UI 细节打磨完成

## PC 端

### 空状态补充（3 页）
- **hr/AccountManagement.vue** — 无子账号时显示 `<el-empty>`
- **teacher/Dashboard.vue** — 无近期投递时显示 `<el-empty>`
- **admin/Dashboard.vue** — 无最近动态时显示 `<el-empty>`

### 缺失样式补充（1 页）
- **hr/CandidateManage.vue** — 补充 `.stat-grid`、`.stat-card`、`.empty-state`、`.no-data` 样式

### 表格 tooltip（14 个文件）
所有 `min-width` 列均已添加 `show-overflow-tooltip`，内容过长时鼠标悬停可查看完整文字。

## 小程序端

### 骨架屏/加载态（9 页）
- **teacher/classes.vue** — 骨架屏
- **teacher/jobs.vue** — 骨架屏
- **teacher/students.vue** — 骨架屏
- **hr/interviews.vue** — 骨架屏
- **hr/jobs.vue** — 骨架屏
- **student/deliveries.vue** — 骨架屏
- **student/messages.vue** — 骨架屏
- **student/home.vue** — 待添加（不同布局）
- **hr/deliveries.vue** — 待添加（Options API 不同模式）

> 注：student/home.vue 和 hr/deliveries.vue 布局特殊，骨架屏需要单独适配，后续补上。
