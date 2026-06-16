# 后端文档索引

> 项目: 职业院校校企招聘与就业管理平台
> 最后更新: 2026-06-08

---

## 📂 文档结构

```
backend/docs/
├── overview.md                  ← 【从这里开始】项目总览
├── README.md                    ← 本文档（索引）
├── config/
│   ├── security.md              ← JWT/Redis/CORS/全局异常配置
│   └── database.md              ← 数据库完整表结构
└── modules/
    ├── auth-module.md           ← 认证模块（登录/注册/JWT）
    ├── user-module.md           ← 用户管理模块
    ├── company-module.md        ← 企业管理模块
    ├── job-module.md            ← 岗位管理模块
    ├── resume-module.md         ← 简历管理模块
    ├── delivery-module.md       ← 投递管理模块
    ├── class-module.md          ← 班级管理模块
    ├── ai-module.md             ← AI智能模块（解析/评分/匹配）
    └── operation-log-module.md  ← 操作日志模块
```

## 📖 阅读顺序

1. **新开发者** → `overview.md`（项目全貌）→ 按需阅读模块文档
2. **定位问题** → 直接阅读对应模块的 `实现文档（函数级）` 部分
3. **修改代码** → 修改前查阅对应模块的原理文档确保不破坏设计意图
4. **添加功能** → 阅读 `config/database.md` 了解现有表结构，再阅读相关模块文档

## 🔗 模块间依赖

```
认证 → 用户 → 班级 → 学生 → 简历 → AI解析
       ↓       ↓                 ↓
     企业 → 岗位 → 投递 → AI评分/AI匹配
              ↓
         操作日志（全局AOP记录）
```

---

> **规范**: 每修改一个 Java 文件，同步更新对应模块文档中的函数行号信息。
