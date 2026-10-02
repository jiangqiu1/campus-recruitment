// 全站展示格式统一工具（界面评审：薪资/时间/未知兜底）
// 规则来源：第三轮界面改造「测试数据与文案卫生」

// 薪资统一为「5K–8K」「150–200元/天」形式：大写 K + en dash（–）
export function formatSalary(raw) {
  if (raw === null || raw === undefined) return ''
  let s = String(raw).trim()
  if (!s) return ''
  if (s === '面议') return '面议'
  return s.replace(/k/g, 'K').replace(/\s*[~～-]\s*/g, '\u2013')
}

// 时间语义化：今天 14:30 / 昨天 18:20 / 周三 14:30 / 7月10日 / 2025年12月20日
// 纯日期字符串（长度<=10）不带时分
export function formatTimeSemantic(t) {
  if (!t) return ''
  const s = String(t).replace('T', ' ')
  const hasTime = s.length > 10
  const d = new Date(hasTime ? s.replace(' ', 'T') : s + 'T00:00')
  if (isNaN(d.getTime())) return s
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  const hm = pad(d.getHours()) + ':' + pad(d.getMinutes())
  const startOf = (x) => new Date(x.getFullYear(), x.getMonth(), x.getDate()).getTime()
  const dayDiff = Math.round((startOf(d) - startOf(now)) / 86400000)
  const weekNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  if (dayDiff === 0) return hasTime ? '今天 ' + hm : '今天'
  if (dayDiff === -1) return hasTime ? '昨天 ' + hm : '昨天'
  if (dayDiff === 1) return hasTime ? '明天 ' + hm : '明天'
  if (dayDiff < 0 && dayDiff >= -6) return weekNames[d.getDay()] + (hasTime ? ' ' + hm : '')
  if (d.getFullYear() === now.getFullYear()) return (d.getMonth() + 1) + '月' + d.getDate() + '日' + (hasTime ? ' ' + hm : '')
  return d.getFullYear() + '年' + (d.getMonth() + 1) + '月' + d.getDate() + '日'
}

// 「未知」兜底收敛：字段缺失时返回空（由调用方决定隐藏或显示占位）
export function ifKnown(v) {
  if (!v || String(v).trim() === '' || String(v) === '未知') return ''
  return String(v)
}
