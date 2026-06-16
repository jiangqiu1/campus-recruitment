# 前端路由与布局配置文档

> 文件: `src/router/index.js` / `src/layouts/AdminLayout.vue` / `src/layouts/TeacherLayout.vue` / `src/layouts/HRLayout.vue`
> 最后更新: 2026-06-08

---

## 一、原理文档

### 1. 路由设计

采用 **Vue Router 4** 的嵌套路由模式，按角色拆分布局：

```
/login                    → Login.vue (独立)
/admin/**                 → AdminLayout.vue 包裹 (管理员PC后台)
  /admin/dashboard        → 数据大屏
  /admin/users            → 用户管理
  /admin/companies        → 企业管理
  /admin/classes          → 班级管理
  /admin/audit            → 企业审核
  /admin/logs             → 操作日志
  /admin/export           → 数据导出
  /admin/settings         → 系统设置
/teacher/**               → TeacherLayout.vue 包裹 (教师PC后台)
  /teacher/dashboard      → 工作台
  /teacher/classes        → 班级管理
  /teacher/jobs           → 岗位发布
  /teacher/deliveries     → 投递看板
/hr/**                    → HRLayout.vue 包裹 (HR PC后台)
  /hr/resumes             → 批量简历处理
  /hr/stats               → 数据统计
  /hr/analysis            → 岗位分析
  /hr/accounts            → 账号管理
```

### 2. 路由守卫逻辑

```javascript
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userRole = localStorage.getItem('userRole')

  // 登录页：已登录则跳转对应后台
  if (to.path === '/login') {
    if (token) {
      // 根据 role 跳转
      userRole === 'admin'   → /admin/dashboard
      userRole === 'teacher' → /teacher/dashboard
      userRole === 'hr'      → /hr/resumes
    }
  } else {
    // 非登录页：校验 Token
    if (!token) → 跳转 /login
    if (to.meta.role !== userRole) → 无权限提示
  }
})
```

### 3. 菜单与路由的对应关系

**侧边栏菜单**在 Layout 组件中硬编码：
- `AdminLayout.vue` — 8个菜单项（el-menu）
- `TeacherLayout.vue` — 4个菜单项
- `HRLayout.vue` — 4个菜单项

每个菜单项使用 `router-link` 或 `el-menu-item` 的 `index` 属性绑定路由 path。

---

## 二、实现文档（函数级）

### 2.1 路由配置文件

**文件**: `src/router/index.js` (~100行)

| 行号 | 路由 | 组件 | 元信息 |
|------|------|------|--------|
| ~45-50 | `/login` | Login.vue | — |
| ~55-110 | `/admin` | AdminLayout.vue (8个子路由) | role: admin |
| ~115-145 | `/teacher` | TeacherLayout.vue (4个子路由) | role: teacher |
| ~150-180 | `/hr` | HRLayout.vue (4个子路由) | role: hr |
| ~185 | `/` → redirect `/login` | — | — |
| ~190 | 404 → redirect `/login` | — | — |

关键函数：
| 行号 | 函数/逻辑 | 功能 |
|------|----------|------|
| ~195-230 | `router.beforeEach()` | 全局路由守卫 |

### 2.2 AdminLayout.vue

**文件**: `src/layouts/AdminLayout.vue`

| 行号 | 菜单项 | 路由路径 | 图标 |
|------|--------|----------|------|
| ~20 | 数据大屏 | `/admin/dashboard` | DataAnalysis / Monitor |
| ~30 | 用户管理 | `/admin/users` | User / UserFilled |
| ~40 | 企业管理 | `/admin/companies` | OfficeBuilding / Shop |
| ~50 | 班级管理 | `/admin/classes` | Reading / School |
| ~60 | 企业审核 | `/admin/audit` | Document / DocumentChecked |
| ~70 | 操作日志 | `/admin/logs` | Notebook / List |
| ~80 | 数据导出 | `/admin/export` | Download / Upload |
| ~90 | 系统设置 | `/admin/settings` | Setting / Tools |

### 2.3 TeacherLayout.vue

**文件**: `src/layouts/TeacherLayout.vue`

| 行号 | 菜单项 | 路由路径 | 功能说明 |
|------|--------|----------|----------|
| ~20 | 工作台 | `/teacher/dashboard` | 数据概览、待办事项 |
| ~30 | 班级管理 | `/teacher/classes` | 查看学生、分配班级 |
| ~40 | 岗位发布 | `/teacher/jobs` | 新增/编辑岗位 |
| ~50 | 投递看板 | `/teacher/deliveries` | 查看学生投递动态 |

### 2.4 HRLayout.vue

**文件**: `src/layouts/HRLayout.vue`

| 行号 | 菜单项 | 路由路径 | 功能说明 |
|------|--------|----------|----------|
| ~20 | 批量简历处理 | `/hr/resumes` | 查看/评分简历 |
| ~30 | 数据统计 | `/hr/stats` | 招聘数据图表 |
| ~40 | 岗位分析 | `/hr/analysis` | 岗位需求分析 |
| ~50 | 账号管理 | `/hr/accounts` | HR账号/信息管理 |

---

## 三、布局组件结构

### 3.1 通用布局模式

```
<el-container>
  <el-aside>              ← 侧边栏（固定宽度 ~200px）
    <el-menu>             ← 导航菜单（v-for 渲染）
  </el-aside>
  <el-container>
    <el-header>           ← 顶部栏（用户信息 + 退出登录）
      <Breadcrumb />      ← 面包屑组件
    </el-header>
    <el-main>
      <router-view />     ← 页面内容
    </el-main>
  </el-container>
</el-container>
```

### 3.2 Breadcrumb 组件

**文件**: `src/components/Breadcrumb.vue`

根据当前路由的 `matched` 数组动态生成面包屑导航：
```javascript
const route = useRoute()
const breadcrumbs = computed(() => {
  return route.matched.filter(item => item.meta?.title)
})
```

---

> **修改日志**: 首次全量梳理产出
