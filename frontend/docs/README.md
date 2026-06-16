# 前端文档索引

> 项目: 职业院校校企招聘与就业管理平台 — 前端
> 最后更新: 2026-06-08

---

## 📂 文档结构

```
frontend/docs/
├── overview.md                  ← 【从这里开始】前端总览
├── README.md                    ← 本文档（索引）
├── config/
│   ├── router.md                ← 路由与布局配置
│   └── api-and-store.md         ← API 层与 Pinia Store
└── modules/
    ├── admin-module.md          ← 管理员模块（PC后台 8个页面）
    ├── teacher-module.md        ← 教师模块（PC后台 4个页面）
    ├── hr-module.md             ← HR 模块（PC后台 4个页面）
    └── miniprogram-module.md    ← 小程序模块（11个页面）
```

## 📖 阅读顺序

1. **新开发者** → `overview.md`（前端项目全貌）→ `config/router.md`
2. **定位问题** → 找到对应模块文档，看实现部分
3. **修改 API** → `config/api-and-store.md` 查看 API 模块定义
4. **添加页面** → `config/router.md` 查看路由配置规范

## 🔗 模块间依赖

```
Login.vue → userStore → authAPI → 后端
             ↓
         路由守卫 → 跳转对应角色后台
             ↓
   ┌───────┴───────┐
   │               │
 AdminLayout   TeacherLayout  HRLayout
    ↓               ↓            ↓
 8个子路由       4个子路由     4个子路由
    ↓               ↓            ↓
 API 层 (12个模块) + Pinia (4个 Store)
```

---

> **规范**: 每修改一个 Vue/JavaScript 文件，同步更新对应模块文档。
