# 数据库设计文档

> 数据库: MySQL 8.0 / 库名: campus_recruitment
> 连接: jdbc:mysql://localhost:3306/campus_recruitment
> ORM: MyBatis-Plus 3.5.5 (全局配置: 驼峰映射 + 逻辑删除 + 自动ID)

---

## 一、ER 关系图（文本描述）

```
┌────────────┐    ┌─────────────────┐    ┌──────────────┐
│  sys_user  │◄───│ student_class   │───►│    class     │
│            │    │                 │    │              │
│  学生(1)   │    │ (多对多中间表)   │    │ 班级         │
│  教师(2)   │    └─────────────────┘    └──────────────┘
│  管理员(3) │
│  HR(4)     │    ┌─────────────┐         ┌──────────────┐
│            │◄───│   resume    │────────►│ job_match_rec│
└────────────┘    │             │         │              │
                  │ (学生→简历)  │         │ 人岗匹配      │
                  └─────────────┘         └──────────────┘
┌────────────┐    ┌──────────────┐
│  company   │───►│     job      │
│            │    │              │
│ 企业       │    │ 岗位         │
└────────────┘    └──────┬───────┘
                         │
                    ┌────▼───────┐    ┌─────────────────┐
                    │  delivery  │    │  job_change_apply│
                    │            │    │                  │
                    │ 投递记录    │    │ 岗位变更申请      │
                    └────────────┘    └──────────────────┘
```

---

## 二、完整表结构

### 2.1 sys_user — 系统用户
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | 用户ID |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 用户名 |
| password | VARCHAR(255) | NOT NULL | BCrypt加密密码 |
| real_name | VARCHAR(50) | — | 真实姓名 |
| role | TINYINT | NOT NULL | 1=学生, 2=教师, 3=管理员, 4=HR |
| phone | VARCHAR(20) | — | AES加密手机号 |
| email | VARCHAR(100) | — | 邮箱 |
| id_card | VARCHAR(18) | — | AES加密身份证号 |
| gender | TINYINT | — | 0=未知, 1=男, 2=女 |
| avatar_url | VARCHAR(255) | — | 头像URL |
| wechat_openid | VARCHAR(100) | — | 微信OpenID |
| status | TINYINT | DEFAULT 1 | 0=禁用, 1=启用 |
| deleted | TINYINT | DEFAULT 0 | 逻辑删除 |
| create_time | DATETIME | — | 创建时间 |
| update_time | DATETIME | — | 更新时间 |

### 2.2 company — 企业
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | 企业ID |
| name | VARCHAR(100) | NOT NULL | 企业名称 |
| description | TEXT | — | 企业简介 |
| address | VARCHAR(255) | — | 企业地址 |
| logo_url | VARCHAR(255) | — | Logo URL |
| contact_name | VARCHAR(50) | — | 联系人 |
| contact_phone | VARCHAR(20) | — | 联系电话 |
| contact_email | VARCHAR(100) | — | 联系邮箱 |
| status | TINYINT | DEFAULT 0 | 0=待审核, 1=通过, 2=拒绝 |
| scale | VARCHAR(50) | — | 企业规模 |
| industry | VARCHAR(50) | — | 所属行业 |
| website | VARCHAR(255) | — | 企业官网 |
| deleted | TINYINT | DEFAULT 0 | 逻辑删除 |

### 2.3 job — 岗位
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | 岗位ID |
| company_id | BIGINT | FK→company.id | 所属企业 |
| title | VARCHAR(100) | NOT NULL | 岗位名称 |
| description | TEXT | — | 岗位描述 |
| requirement | TEXT | — | 任职要求 |
| salary_min | DECIMAL(10,2) | — | 薪资下限 |
| salary_max | DECIMAL(10,2) | — | 薪资上限 |
| location | VARCHAR(100) | — | 工作地点 |
| type | VARCHAR(50) | — | 工作类型(全职/实习) |
| head_count | INT | DEFAULT 1 | 招聘人数 |
| status | TINYINT | DEFAULT 1 | 0=关闭, 1=开放 |
| deadline | DATETIME | — | 截止日期 |
| is_ai_match_enabled | TINYINT | DEFAULT 0 | 是否启用AI匹配 |
| deleted | TINYINT | DEFAULT 0 | 逻辑删除 |

### 2.4 resume — 简历
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | 简历ID |
| student_id | BIGINT | FK→sys_user.id | 学生ID |
| content | TEXT | — | 简历文本内容 |
| file_url | VARCHAR(255) | — | PDF文件URL |
| pdf_url | VARCHAR(255) | — | PDF路径 |
| skill_tags | VARCHAR(500) | — | 技能标签(逗号分隔) |
| job_intention | VARCHAR(100) | — | 求职意向 |
| is_default | TINYINT | DEFAULT 0 | 是否默认 |
| status | TINYINT | DEFAULT 0 | 0=编辑中, 1=已完成 |
| version | INT | DEFAULT 1 | 版本号 |
| deleted | TINYINT | DEFAULT 0 | 逻辑删除 |

### 2.5 delivery — 投递记录
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | 投递ID |
| student_id | BIGINT | FK→sys_user.id | 学生ID |
| job_id | BIGINT | FK→job.id | 岗位ID |
| status | TINYINT | DEFAULT 0 | 0=已投递, 1=已查看, 2=待面试, 3=已录用, 4=不合适 |
| resume_version | VARCHAR(50) | — | 简历版本号 |
| feedback | TEXT | — | HR反馈 |
| interview_time | DATETIME | — | 面试时间 |
| interview_location | VARCHAR(255) | — | 面试地点 |
| deleted | TINYINT | DEFAULT 0 | 逻辑删除 |
| create_time | DATETIME | — | 创建时间 |
| update_time | DATETIME | — | 更新时间 |

### 2.6 class — 班级
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | 班级ID |
| name | VARCHAR(100) | NOT NULL | 班级名称 |
| grade | VARCHAR(50) | — | 年级 |
| teacher_id | BIGINT | FK→sys_user.id | 班主任(教师ID) |
| description | TEXT | — | 班级简介 |
| deleted | TINYINT | DEFAULT 0 | 逻辑删除 |

### 2.7 student_class — 学生班级关联
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | ID |
| student_id | BIGINT | FK→sys_user.id | 学生ID |
| class_id | BIGINT | FK→class.id | 班级ID |

### 2.8 job_change_apply — 岗位变更申请
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | ID |
| job_id | BIGINT | FK→job.id | 岗位ID |
| applicant_id | BIGINT | FK→sys_user.id | 申请人ID |
| change_type | VARCHAR(50) | — | 变更类型 |
| reason | TEXT | — | 变更原因 |
| status | TINYINT | DEFAULT 0 | 0=待审批, 1=通过, 2=拒绝 |

### 2.9 operation_log — 操作日志
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | ID |
| user_id | BIGINT | FK→sys_user.id | 操作人 |
| operation_type | VARCHAR(50) | NOT NULL | 操作类型 |
| target | VARCHAR(100) | — | 操作对象 |
| detail | TEXT | — | 操作详情 |
| ip_address | VARCHAR(50) | — | IP地址 |
| create_time | DATETIME | — | 操作时间 |

### 2.10 ai_parse_log — AI解析日志
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | ID |
| resume_id | BIGINT | FK→resume.id | 简历ID |
| parse_content | TEXT | — | 解析结果 |
| parse_time | DATETIME | — | 解析时间 |
| status | TINYINT | — | 0=待解析, 1=解析中, 2=完成, 3=失败 |
| error_message | VARCHAR(500) | — | 错误信息 |

### 2.11 ai_feedback_log — AI反馈日志
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | ID |
| parse_log_id | BIGINT | FK→ai_parse_log.id | 关联的解析日志 |
| feedback_type | VARCHAR(50) | — | 反馈类型 |
| feedback_content | TEXT | — | 反馈内容 |
| rating | TINYINT | — | 评分(1-5) |
| user_id | BIGINT | — | 反馈用户 |
| create_time | DATETIME | — | 反馈时间 |

### 2.12 job_match_record — 人岗匹配记录
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | ID |
| job_id | BIGINT | FK→job.id | 岗位ID |
| resume_id | BIGINT | FK→resume.id | 简历ID |
| match_score | DECIMAL(5,2) | — | 匹配分数(0-100) |
| match_detail | TEXT | — | 匹配详情 |
| status | TINYINT | DEFAULT 0 | 0=未推送, 1=已推送 |
| create_time | DATETIME | — | 创建时间 |

### 2.13 resume_score_log — 简历评分记录
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | ID |
| resume_id | BIGINT | FK→resume.id | 简历ID |
| job_id | BIGINT | FK→job.id | 岗位ID |
| total_score | DECIMAL(5,2) | — | 总分 |
| dimension_scores | TEXT | — | 维度评分(JSON) |
| score_detail | TEXT | — | 评分详情 |
| score_time | DATETIME | — | 评分时间 |
| deleted | TINYINT | DEFAULT 0 | 逻辑删除 |

---

## 三、MyBatis-Plus 配置

```yaml
mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.recruit.entity
  configuration:
    map-underscore-to-camel-case: true    # sys_user → SysUser 驼峰映射
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # SQL日志（开发环境）
  global-config:
    db-config:
      id-type: AUTO                         # 自增主键
      logic-delete-field: deleted           # 逻辑删除字段名
      logic-delete-value: 1                 # 逻辑已删除值
      logic-not-delete-value: 0             # 逻辑未删除值
```

---

> **修改日志**: 首次全量梳理产出
