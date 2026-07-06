import { request } from './request'

/**
 * 添加浏览记录
 * 调用后端 API 存入数据库
 */
export const addBrowseRecord = (jobId) => {
	if (!jobId) return
	try {
		// 从登录信息中获取学生ID
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return
		const userInfo = JSON.parse(raw)
		const studentId = userInfo.id || userInfo.userId
		if (!studentId) return

		request({
			url: '/browse-history',
			method: 'POST',
			data: { studentId: Number(studentId), jobId: Number(jobId) }
		}).catch(() => {})
	} catch (e) {}
}

/**
 * 获取浏览历史列表
 */
export const getBrowseHistory = async () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return []
		const userInfo = JSON.parse(raw)
		const studentId = userInfo.id || userInfo.userId
		if (!studentId) return []

		const res = await request({ url: '/browse-history?studentId=' + studentId })
		return (res.data || []).map(item => ({
			jobId: item.jobId,
			viewTime: item.createTime ? item.createTime.substring(0, 16).replace('T', ' ') : ''
		}))
	} catch (e) { return [] }
}

/**
 * 批量删除浏览记录
 */
export const clearBrowseHistory = async (ids) => {
	try {
		if (ids && ids.length) {
			await request({ url: '/browse-history/batch-delete', method: 'POST', data: ids })
		} else {
			const raw = uni.getStorageSync('userInfo')
			if (!raw) return
			const userInfo = JSON.parse(raw)
			const studentId = userInfo.id || userInfo.userId
			if (studentId) {
				await request({ url: '/browse-history/clear?studentId=' + studentId, method: 'DELETE' })
			}
		}
	} catch (e) {}
}
