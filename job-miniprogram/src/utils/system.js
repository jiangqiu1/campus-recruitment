export function getStatusBarHeight() {
	const systemInfo = uni.getSystemInfoSync()
	return systemInfo.statusBarHeight || 20 // 兼容旧机型
}
