<template>
	<view class="page-wrapper">
		<NavBar title="隐私政策" show-back />
		<scroll-view class="content-scrollable" scroll-y>
			<view class="content-card">
				<text class="title">职业院校招聘就业平台 隐私政策</text>
				<text class="update-time">更新日期：2026年1月1日</text>
				<text class="section-title">一、引言</text>
				<text class="paragraph">我们深知个人信息对您的重要性，将严格按照法律法规要求，采取相应安全保护措施，尽力保护您的个人信息安全可控。</text>
				<text class="section-title">二、我们收集的信息</text>
				<text class="paragraph">1. 注册信息：您注册账号时提供的姓名、学号/工号、手机号、邮箱、院校/企业信息等。</text>
				<text class="paragraph">2. 简历信息：您主动填写的教育经历、实习经历、技能证书、求职意向、自我评价等求职相关信息。</text>
				<text class="paragraph">3. 使用信息：您的浏览记录、投递记录、收藏记录、设备信息、日志信息，用于优化服务体验。</text>
				<text class="paragraph">4. 位置信息：经您授权后获取的大致位置，用于岗位本地化推荐。</text>
				<text class="section-title">三、信息使用方式</text>
				<text class="paragraph">1. 用于向您提供岗位匹配、简历投递、消息通知等核心服务。</text>
				<text class="paragraph">2. 用于AI算法分析，优化推荐精准度，提升求职效率。</text>
				<text class="paragraph">3. 用于院校就业统计分析，数据均做匿名化、聚合化处理。</text>
				<text class="paragraph">4. 用于账号安全验证、风险防控。</text>
				<text class="section-title">四、信息共享与披露</text>
				<text class="paragraph">1. 经您同意后，您的简历信息将展示给投递岗位对应的企业HR。</text>
				<text class="paragraph">2. 向合作院校提供匿名化的就业统计数据，不包含个人可识别信息。</text>
				<text class="paragraph">3. 法律法规要求、司法机关指令或为保护公共利益时，依法提供相关信息。</text>
				<text class="paragraph">4. 未经您的同意，我们不会向第三方出售、出租您的个人信息。</text>
				<text class="section-title">五、信息存储与安全</text>
				<text class="paragraph">1. 您的信息存储于中国境内服务器，存储期限为实现服务目的所必需的时间。</text>
				<text class="paragraph">2. 我们采用行业标准的安全技术与管理措施，保护您的信息不被泄露、篡改或丢失。</text>
				<text class="paragraph">3. 账号注销后，我们将在合理期限内删除或匿名化您的个人信息。</text>
				<text class="section-title">六、您的权利</text>
				<text class="paragraph">您有权访问、更正、删除您的个人信息，有权撤回授权、注销账号。相关操作可在"账号安全"及对应功能页面完成。</text>
				<text class="section-title">七、未成年人保护</text>
				<text class="paragraph">若您是未满18周岁的未成年人，请在监护人指导下使用本平台，并在监护人同意后提交个人信息。</text>
				<text class="section-title">八、政策更新</text>
				<text class="paragraph">本政策可能适时更新，更新后将在平台内公示，继续使用服务即视为您同意更新后的政策。</text>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import NavBar from '@/components/NavBar.vue'
</script>

<style scoped lang="scss">
.content-card { background: $uni-bg-color; margin: 16px; padding: 20px; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); line-height: 1.8; }
.title { font-size: 18px; font-weight: 700; color: $uni-text-color-title; display: block; text-align: center; margin-bottom: 8px; }
.update-time { font-size: 12px; color: $uni-text-color-placeholder; display: block; text-align: center; margin-bottom: 20px; }
.section-title { font-size: 15px; font-weight: 600; color: $uni-text-color-title; display: block; margin-top: 20px; margin-bottom: 8px; }
.paragraph { font-size: 14px; color: $uni-text-color; display: block; margin-bottom: 8px; text-indent: 2em; }
</style>
