/**
 * 格式化日期时间字符串
 * 统一处理 ISO 时间格式，避免全项目 T/空格/截取 不一致
 *
 * @param {string} dateStr - ISO 时间字符串 (如 "2026-07-08T14:30:00")
 * @param {object} options
 * @param {boolean} options.showSeconds - 是否显示秒 (default: false)
 * @param {string} options.emptyText - 为空时的回退文本 (default: '-')
 * @returns {string} 格式化后的时间字符串 (如 "2026-07-08 14:30")
 */
export function formatDate(dateStr, options = {}) {
  const { showSeconds = false, emptyText = '-' } = options
  if (!dateStr) return emptyText
  const len = showSeconds ? 19 : 16
  return dateStr.substring(0, len).replace('T', ' ')
}

/**
 * 判断时间是否已过期（早于当前时间）
 *
 * @param {string} dateStr - ISO 时间字符串
 * @returns {boolean}
 */
export function isExpired(dateStr) {
  if (!dateStr) return false
  return new Date(dateStr) < new Date()
}
