<script>
	export default {
		onLaunch: function() {
			console.log('App Launch')
			const token = uni.getStorageSync('token')
			const userInfoStr = uni.getStorageSync('userInfo')
			if (token && userInfoStr) {
				try {
					const userInfo = JSON.parse(userInfoStr)
					const role = userInfo.role
					const homeMap = {0: '/pages/student/home', 1: '/pages/teacher/home', 2: '/pages/hr/home'}
					uni.reLaunch({ url: homeMap[role] || '/pages/student/home' })
				} catch(e) {
					uni.reLaunch({ url: '/pages/student/login' })
				}
			}
			// 没有 token 时不做跳转，由 pages/index/index 处理
		}
	}
</script>

<style>
/* 全局点按反馈：按钮按压淡出（页面可按需覆盖） */
button {
	transition: opacity 0.2s ease;
}
button:active {
	opacity: 0.85;
}

/* 全局基础样式 - 补充规范 */
page {
	background-color: #F7F8FA;
	height: 100%;
	font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
	-webkit-tap-highlight-color: transparent;
}

/* 所有 view 默认纵向 flex，方便布局 */
view {
	display: flex;
	flex-direction: column;
	box-sizing: border-box;
}

scroll-view {
	background: #F7F8FA;
}

/* 可滚动容器统一风格 */
.content-scrollable {
	flex: 1;
	overflow-y: auto;
	overflow-x: hidden;
	background: #F7F8FA;
}

/* 页面根容器，确保占满高度 */
.page-wrapper {
	width: 100%;
	height: 100%;
	display: flex;
	flex-direction: column;
	background: #F7F8FA;
}
</style>
