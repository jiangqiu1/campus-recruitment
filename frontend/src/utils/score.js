/**
 * 评分数据契约统一工具
 *
 * 背景：项目中 AI 评分体系三处各自实现，存在两个问题——
 *   P2-7 评分刻度不统一：matchScore 存 0-1 小数，score 存 0-100 整数
 *   P2-8 scoreDetail 维度契约不统一：中文 key / 英文 key / 英文简写三套并存
 *
 * 统一约定（本模块为唯一入口）：
 *   1. 展示层分数一律使用 0-100 整数
 *   2. scoreDetail 内部标准维度 key：skill / exp / edu / major / salary / stability
 *   3. 颜色阈值统一：>=90 优秀 / >=75 合格 / 其余待提升
 *
 * 修改评分相关逻辑时，请优先复用本模块，避免再次出现多套实现。
 */

// 标准维度 key → 中文标签
export const SCORE_DIM_LABELS = {
  skill: '技能匹配',
  exp: '经验匹配',
  edu: '学历匹配',
  major: '专业契合',
  salary: '薪资匹配',
  stability: '稳定性'
}

// 各数据源的 key 别名 → 标准维度 key（兼容中文 key、英文 key、英文简写）
const DIM_ALIASES = {
  // 简历评分（ResumeScoreLog.scoreDetail，中文 key）
  '技能得分': 'skill',
  '经验得分': 'exp',
  '教育得分': 'edu',
  '薪资匹配': 'salary',
  '稳定性': 'stability',
  // 人岗匹配（JobMatchRecord.scoreDetail，英文 key）
  'skillMatch': 'skill',
  'expMatch': 'exp',
  'eduMatch': 'edu',
  'majorFit': 'major',
  // 英文简写（JobAnalysis 等历史兼容）
  'skills': 'skill',
  'experience': 'exp',
  'education': 'edu',
  'salary': 'salary',
  // 总评与评语
  '总分': 'total',
  'totalScore': 'total',
  '评语': 'comment',
  'comment': 'comment'
}

// 标准维度展示顺序
const DIM_ORDER = ['skill', 'exp', 'edu', 'major', 'salary', 'stability']

/**
 * 归一化分数到 0-100 整数（用于 score 字段，本身已是 0-100）。
 * 非法输入返回 null。
 */
export function normalizeScore(value) {
  if (value === null || value === undefined || value === '') return null
  const n = typeof value === 'number' ? value : parseFloat(value)
  if (Number.isNaN(n)) return null
  return Math.max(0, Math.min(100, Math.round(n)))
}

/**
 * 匹配分转百分比：JobMatchRecord.matchScore 存的是 0-1 小数，转为 0-100 整数。
 * 若传入值已 > 1（历史/防御），视为 0-100 直接取整。
 */
export function matchScoreToPercent(value) {
  if (value === null || value === undefined || value === '') return 0
  const n = typeof value === 'number' ? value : parseFloat(value)
  if (Number.isNaN(n)) return 0
  const p = n <= 1 ? n * 100 : n
  return Math.max(0, Math.min(100, Math.round(p)))
}

/**
 * 将任意维度 key（中文/英文/简写）转为标准中文标签。
 * 未识别的 key 原样返回。
 */
export function dimLabel(key) {
  const std = DIM_ALIASES[key] || key
  return SCORE_DIM_LABELS[std] || key
}

// 在对象中查找某标准维度在任意别名下的原始值
function findDimValue(sd, stdKey) {
  for (const [alias, std] of Object.entries(DIM_ALIASES)) {
    if (std === stdKey && sd[alias] !== undefined && sd[alias] !== null) {
      return sd[alias]
    }
  }
  return null
}

/**
 * 解析 scoreDetail（JSON 字符串或对象），返回统一结构：
 *   { total: number|null, comment: string, dims: [{ key, label, score }] }
 * 兼容中文 key 与英文 key，维度按 DIM_ORDER 排序。
 * @param detail scoreDetail 原始值
 * @param fallbackTotal 当 detail 中无总分时的兜底（通常传 score 字段，0-100）
 */
export function parseScoreDetail(detail, fallbackTotal = null) {
  let sd = detail
  if (typeof sd === 'string') {
    try { sd = JSON.parse(sd) } catch { sd = null }
  }
  if (!sd || typeof sd !== 'object') {
    return { total: normalizeScore(fallbackTotal), comment: '', dims: [] }
  }

  let total = normalizeScore(sd.total)
  if (total === null) total = normalizeScore(sd.总分)
  if (total === null) total = normalizeScore(sd.totalScore)
  if (total === null) total = normalizeScore(fallbackTotal)

  const comment = sd.评语 || sd.comment || ''

  const dims = []
  for (const stdKey of DIM_ORDER) {
    const val = findDimValue(sd, stdKey)
    if (val !== null) {
      const score = normalizeScore(val)
      if (score !== null) {
        dims.push({ key: stdKey, label: SCORE_DIM_LABELS[stdKey], score })
      }
    }
  }
  return { total, comment, dims }
}

/**
 * 统一评分颜色：>=90 优秀(绿) / >=75 合格(橙) / 其余待提升(红) / 无效 灰。
 * 入参必须是 0-100 分数（matchScore 请先用 matchScoreToPercent 转换）。
 */
export function scoreColor(score) {
  const s = normalizeScore(score)
  if (s === null) return '#C9CDD4'
  if (s >= 90) return '#67C23A'
  if (s >= 75) return '#E6A23C'
  return '#F56C6C'
}

/**
 * 统一评分标签类型（Element Plus tag type）。
 */
export function scoreTag(score) {
  const s = normalizeScore(score)
  if (s === null) return 'info'
  if (s >= 90) return 'success'
  if (s >= 75) return 'warning'
  return 'danger'
}
