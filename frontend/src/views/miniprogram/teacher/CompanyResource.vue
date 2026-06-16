<template>
  <div class="company-resource">
    <div class="card">
      <div class="card-header">
        <h2>合作企业资源库</h2>
      </div>
      
      <table class="data-table">
        <thead>
          <tr>
            <th>企业名称</th>
            <th>行业</th>
            <th>合作状态</th>
            <th>录用学生</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="company in companyList" :key="company.id">
            <td>{{ company.name }}</td>
            <td>{{ company.industry }}</td>
            <td>
              <span :class="getCooperationClass(company.cooperationLevel)" class="tag">
                {{ getCooperationText(company.cooperationLevel) }}
              </span>
            </td>
            <td>{{ company.hiredCount }}</td>
            <td>
              <button class="btn btn-outline">查看详情</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { companyAPI } from '@/api'

const companyList = ref([])

onMounted(async () => {
  await loadCompanies()
})

const loadCompanies = async () => {
  try {
    const res = await companyAPI.getAllCompanies()
    if (res.code === 200) {
      companyList.value = res.data || []
    }
  } catch (error) {
    console.error('加载企业列表失败', error)
  }
}

const getCooperationClass = (level) => {
  if (level >= 3) return 'tag-success'
  if (level >= 2) return 'tag-warning'
  return 'tag-info'
}

const getCooperationText = (level) => {
  if (level >= 3) return '核心合作'
  if (level >= 2) return '正式合作'
  return '初步合作'
}
</script>

<style scoped>
.company-resource {
  padding: 20px;
}

.card {
  background: white;
  border-radius: 16px;
  padding: 32px;
  margin-bottom: 28px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.06);
  position: relative;
}

.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, #165DFF, #2563EB, transparent);
  border-radius: 16px 16px 0 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.card h2 {
  font-size: 19px;
  font-weight: 600;
  padding-bottom: 12px;
  border-bottom: 1px solid #F2F3F5;
  position: relative;
}

.card h2::after {
  content: '';
  width: 50px;
  height: 3px;
  background: #165DFF;
  border-radius: 3px;
  position: absolute;
  left: 0;
  bottom: -1px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 16px;
}

.data-table th {
  text-align: left;
  padding: 14px 12px;
  background: #F8F9FC;
  color: #4E5969;
  font-weight: 600;
  font-size: 14px;
  border-bottom: 2px solid #E2E8F0;
}

.data-table td {
  padding: 14px 12px;
  border-bottom: 1px solid #F2F3F5;
  font-size: 14px;
  color: #1D2129;
}

.data-table tr:hover td {
  background: rgba(22, 93, 255, 0.03);
}

.tag {
  display: inline-block;
  padding: 7px 13px;
  background: #F8F9FC;
  border-radius: 8px;
  font-size: 13px;
  margin-right: 8px;
  margin-bottom: 8px;
  color: #4E5969;
  border: 1px solid #E2E8F0;
}

.tag-success {
  background: #D1FAE5;
  color: #059669;
  border-color: #A7F3D0;
}

.tag-warning {
  background: #FEF3C7;
  color: #D97706;
  border-color: #FDE68A;
}

.tag-info {
  background: #E0F2FE;
  color: #0369A1;
  border-color: #BAE6FD;
}

.btn-outline {
  padding: 6px 12px;
  font-size: 13px;
  background: #fff;
  border: 1px solid #DCDFE6;
  color: #4E5969;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s;
}

.btn-outline:hover {
  background: #F5F7FA;
  border-color: #165DFF;
  color: #165DFF;
  transform: translateY(-2px);
}
</style>