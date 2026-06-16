# 职业院校校企招聘与就业管理平台 - 前端启动指南

## ✅ 已完成工作

### 1. Pinia 状态管理（已创建）
- `src/stores/user.js` - 用户状态
- `src/stores/app.js` - 应用状态
- `src/stores/resume.js` - 简历状态
- `src/stores/job.js` - 岗位状态

### 2. 布局文件（已创建）
- `src/layouts/AdminLayout.vue` - 管理员布局
- `src/layouts/TeacherLayout.vue` - 教师端布局
- `src/layouts/HRLayout.vue` - HR端布局

### 3. 管理员端页面（已创建）
- `src/views/admin/Dashboard.vue` - 数据大屏
- `src/views/admin/UserManage.vue` - 用户管理
- `src/views/admin/CompanyManage.vue` - 企业管理
- `src/views/admin/ClassManage.vue` - 班级管理
- `src/views/admin/EnterpriseAudit.vue` - 企业审核
- `src/views/admin/OperationLog.vue` - 操作日志
- `src/views/admin/DataExport.vue` - 数据导出
- `src/views/admin/SystemSettings.vue` - 系统设置

### 4. 教师端页面（已创建）
- `src/views/simple-teacher/Dashboard.vue` - 工作台
- `src/views/simple-teacher/ClassManagement.vue` - 班级管理
- `src/views/simple-teacher/ResumeManagement.vue` - 简历管理
- `src/views/simple-teacher/JobPosting.vue` - 岗位发布
- `src/views/simple-teacher/DeliveryBoard.vue` - 投递看板
- `src/views/simple-teacher/CompanyResource.vue` - 企业资源库
- `src/views/simple-teacher/MessageNotification.vue` - 消息通知
- `src/views/simple-teacher/PersonalSettings.vue` - 个人设置

### 5. HR端页面（已创建）
- `src/views/hr/Dashboard.vue` - 工作台
- `src/views/hr/CompanyProfile.vue` - 企业信息
- `src/views/hr/JobManage.vue` - 岗位管理
- `src/views/hr/HRResumeManage.vue` - 简历管理
- `src/views/hr/ResumeScore.vue` - 简历评分
- `src/views/hr/DataStats.vue` - 数据统计
- `src/views/hr/JobAnalysis.vue` - 岗位分析
- `src/views/hr/AccountManagement.vue` - 账号管理

### 6. 组件（已创建）
- `src/components/Breadcrumb.vue` - 面包屑导航

### 7. 数据库脚本（已修复）
- `backend/sql/schema.sql` - 数据库表结构
- `backend/sql/data.sql` - 测试数据（已修复3个问题）

## 🚀 启动步骤

### 前端启动
```powershell
cd E:\校企项目\frontend
npm run dev
```
访问：http://localhost:5173/

### 后端启动（需要Maven）
```powershell
cd E:\校企项目\backend
mvn spring-boot:run
```

### 数据库初始化
```sql
USE campus_recruitment;
source E:/校企项目/backend/sql/schema.sql;
source E:/校企项目/backend/sql/data.sql;
```

## 📝 注意事项

1. **前端页面目前是占位符** - 所有页面显示"页面开发中..."，需要后续完善
2. **需要安装Maven** - 后端编译需要Maven
3. **MySQL和Redis需要启动** - 后端依赖这两个服务

## 🔧 下一步工作

1. 完善前端页面内容
2. 安装Maven并编译后端
3. 对接后端API
4. 添加ECharts图表
5. 实现文件上传功能
