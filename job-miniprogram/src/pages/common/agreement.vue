<template>
	<view class="page-wrapper">
		<NavBar title="用户协议" show-back />
		<scroll-view class="content-scrollable" scroll-y>
			<view class="content-card">
				<text class="title">职业院校招聘就业平台 用户协议</text>
				<text class="update-time">更新日期：2026年1月1日</text>
				<text class="section-title">一、协议范围</text>
				<text class="paragraph">本协议是您与职业院校招聘就业平台（以下简称"本平台"）之间，就您使用本平台提供的各项服务所订立的协议。请您在注册或使用本平台服务前，仔细阅读本协议的全部内容。</text>
				<text class="section-title">二、账号注册与使用</text>
				<text class="paragraph">1. 您承诺以真实身份注册账号，并保证所提供信息的真实性、准确性和完整性。信息发生变更时，应及时更新。</text>
				<text class="paragraph">2. 您应妥善保管账号及密码，对账号下的所有行为承担责任。如发现账号异常，请立即联系平台客服。</text>
				<text class="paragraph">3. 学生账号仅限本人使用，企业HR账号仅限授权人员使用，禁止转借、出租、售卖账号。</text>
				<text class="section-title">三、服务内容</text>
				<text class="paragraph">1. 本平台为职业院校学生、教师及合作企业提供岗位发布、简历投递、AI岗位匹配、就业统计等相关服务。</text>
				<text class="paragraph">2. 平台有权根据业务发展调整服务内容，或对部分服务进行收费，收费服务将提前告知。</text>
				<text class="section-title">四、用户行为规范</text>
				<text class="paragraph">1. 不得发布虚假简历、虚假岗位信息，不得进行欺诈、误导性操作。</text>
				<text class="paragraph">2. 不得利用平台进行任何违法违规活动，不得侵犯他人合法权益。</text>
				<text class="paragraph">3. 不得恶意攻击、刷量、爬虫抓取平台数据，不得干扰平台正常运行。</text>
				<text class="section-title">五、知识产权</text>
				<text class="paragraph">本平台的所有内容，包括但不限于文字、图片、软件、接口、设计等，均受相关法律法规保护，未经授权不得复制、传播或用于商业用途。</text>
				<text class="section-title">六、免责声明</text>
				<text class="paragraph">1. 平台仅提供信息发布与匹配服务，不对企业及个人的实际行为承担担保责任，求职与招聘双方应自行核实信息真实性。</text>
				<text class="paragraph">2. 因不可抗力、网络故障、系统维护等原因导致服务中断，平台不承担责任。</text>
				<text class="paragraph">3. AI匹配结果仅为参考建议，不构成任何求职或招聘建议。</text>
				<text class="section-title">七、协议修改</text>
				<text class="paragraph">平台有权根据法律法规及业务需要修改本协议，修改后的协议将在平台上公布，继续使用服务即视为同意修改后的协议。</text>
				<text class="section-title">八、联系我们</text>
				<text class="paragraph">如您对本协议有任何疑问，可通过平台意见反馈渠道与我们联系。</text>
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
