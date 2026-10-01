// 简历完整度统一口径：7 个分组项，学生端各页面共用
// （基本信息 = 姓名+手机号+邮箱 三项合一）
export function buildResumeChecklist(resume, userInfo = {}) {
	const has = (v) => !!v && String(v).trim() && v !== '[]'
	return [
		{ label: '基本信息', done: !!(userInfo.realName && userInfo.phone && userInfo.email) },
		{ label: '求职意向', done: has(resume && resume.jobTarget) },
		{ label: '教育经历', done: has(resume && resume.education) },
		{ label: '实习经历', done: has(resume && resume.internship) },
		{ label: '项目经历', done: has(resume && resume.project) },
		{ label: '技能证书', done: has(resume && resume.skills) },
		{ label: '自我评价', done: has(resume && resume.selfEvaluation) }
	]
}

export function resumeCompleteness(checklist) {
	if (!checklist || !checklist.length) return 0
	return Math.round((checklist.filter((i) => i.done).length / checklist.length) * 100)
}
