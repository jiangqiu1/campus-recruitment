# Dashboard 数据大屏详解（深度补充）

> 定位：`views/pc/` 下三个角色的数据看板页面。以 admin/Dashboard.vue 为代码级详例，teacher/HR 同构。

## 1. 三个 Dashboard 概览

| 页面 | 行数 | 职责 | 数据源 |
|------|------|------|--------|
| admin/Dashboard.vue | 181 | 管理员数据大屏 | statisticsAPI.getOverview/getDeliveryTrend/getHotJobs/getRecentActivities |
| teacher/Dashboard.vue | 203 | 教师工作台 | statisticsAPI.getTeacherDashboard/getDeliveryTrend/getEmploymentDistribution |
| hr/DataStats.vue | 174 | HR 数据统计 | statisticsAPI.getHrDashboard/getDeliveryTrend + jobAPI/deliveryAPI |

三者结构同构：**顶部指标卡 + ECharts 图表 + 列表/动态**。

## 2. admin/Dashboard 代码级详解（181 行）

### 2.1 页面结构

```
① 4 个指标卡（stat-card）：注册用户 / 入驻企业 / 在招岗位 / 投递简历
② 2 个图表：岗位投递趋势（折线图）+ 热门岗位 Top10（横向柱状图）
③ 最近动态表格（el-table）
```

### 2.2 数据加载（onMounted，76 行）

```js
await Promise.all([loadOverview(), loadDeliveryTrend(), loadHotJobs(), loadRecentActivities()])
await nextTick()
renderCharts()   // 数据渲染完成后再初始化图表
```

四个接口**并发**加载，`nextTick` 确保 DOM 就绪后再挂图表。

### 2.3 折线图（投递趋势，125-141 行）

- 类型：line + smooth + 渐变面积（`LinearGradient` 蓝→透明）。
- xAxis：`trendData.map(d => d.label)`；series data：`trendData.map(d => d.value)`。
- 数据来自 `getDeliveryTrend`，前端 `map` 成 `{label, value}`（100 行）。

### 2.4 横向柱状图（热门岗位，144-162 行）

- 类型：bar 横向，`barWidth: 20`、`borderRadius: [0,4,4,0]`。
- 10 色循环：`['#165DFF','#10B981','#F59E0B',...][i % 10]`。
- 排序：`slice(0,10).sort((a,b) => a.count - b.count)`（升序，让柱状图自上而下由高到低）。

### 2.5 图表生命周期

- `onMounted` 里 `echarts.init(ref)`。
- `onUnmounted` 里 `trendInstance?.dispose()` / `hotInstance?.dispose()`（84-87 行）防内存泄漏。

## 3. ECharts 使用模式（全项目通用）

| 步骤 | 位置 |
|------|------|
| 引入 | `import * as echarts from 'echarts'`（全量引入） |
| 初始化 | `echarts.init(domRef)` |
| 配置 | `setOption({...})` |
| 销毁 | `onUnmounted` 里 `dispose()` |
| 自适应 | 部分页面有 `window.addEventListener('resize', ...)`，部分没有（见已知问题） |

## 4. 已知问题

1. **ECharts 全量引入**：`import * as echarts from 'echarts'` 未按需引入（`echarts/core` + 需要的图表），打包体积大。
2. **部分页面无 resize 监听**：JobAnalysis、AIStats 等页面未绑定 resize，窗口缩放时图表不自适应（admin/Dashboard 也无显式 resize，依赖容器固定高度）。
3. **空数据兜底**：图表仅在 `data.length > 0` 时渲染，空时显示 `chart-empty` 文案（模板 31/36 行）。
