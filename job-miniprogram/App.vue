<script>
	export default {
		onLaunch: function() {
			console.log('App Launch')
			// 检查登录状态
			const token = uni.getStorageSync('token')
			if (token) {
				// 已登录，跳转到首页
				uni.reLaunch({ url: '/pages/student/home' })
			} else {
				// 未登录，跳转到登录页
				uni.reLaunch({ url: '/pages/student/login' })
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
