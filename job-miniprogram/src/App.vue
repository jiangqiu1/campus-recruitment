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
					if (role === 0 || role === '0') {
						uni.reLaunch({ url: '/pages/student/home' })
					} else if (role === 1 || role === '1') {
						uni.reLaunch({ url: '/pages/teacher/home' })
					} else if (role === 2 || role === '2') {
						uni.reLaunch({ url: '/pages/hr/home' })
					} else {
						uni.reLaunch({ url: '/pages/student/home' })
					}
				} catch(e) {
					uni.reLaunch({ url: '/pages/student/login' })
				}
			} else {
				uni.reLaunch({ url: '/pages/index/index' })
			}
		}
	}
</script>

<style>
/* 全局样式 - 匹配HTML原型 */
/* 注意: 微信小程序WXSS不支持 * 通配符 */
page {
	display: flex;
	flex-direction: column;
	margin: 0;
	padding: 0;
	background: #F5F7FA;
	height: 100%;
	font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
	-webkit-tap-highlight-color: transparent;
}
view {
	display: flex;
	flex-direction: column;
}
scroll-view {
	background: #F5F7FA;
}
.content-scrollable {
	flex: 1;
	overflow-y: auto;
	overflow-x: hidden;
	background: #F5F7FA;
}
.page-wrapper {
	width: 100%;
	height: 100%;
	display: flex;
	flex-direction: column;
	background: #F5F7FA;
}
</style>
