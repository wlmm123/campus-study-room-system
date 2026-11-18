<template>
  <div>
    <h2 style="color: #8b5a2b; margin-bottom: 20px;">我的订单</h2>
    
    <el-table :data="orders" border style="width: 100%;" v-loading="loading" :cell-style="{textAlign: 'center'}" :header-cell-style="{textAlign: 'center', background: '#f5f7fa', color: '#606266'}">
      <el-table-column prop="orderId" label="订单号" width="120" />
      <el-table-column prop="seatId" label="座位ID" width="100" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusTag(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="预约开始时间" width="160" />
      <el-table-column prop="endTime" label="预约结束时间" width="160" />
      
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === '待签到'">
            <el-button size="small" class="btn-light-green" @click="handleSignIn(row.orderId)">签到</el-button>
            <el-button size="small" class="btn-outline-brown" @click="handleCancel(row.orderId)">取消预约</el-button>
          </template>
          
          <template v-if="row.status === '已签到'">
            <el-button size="small" class="btn-light-blue" @click="handleTempLeave(row.orderId)">暂离</el-button>
            <el-button size="small" class="btn-outline-brown" @click="handleLeave(row.orderId)">主动离开</el-button>
          </template>
          
          <span v-if="['已完成', '已取消', '超时释放', '暂离超时释放'].includes(row.status)" style="color: #909399; font-size: 13px;">
            订单已结束
          </span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getMyOrders, signInOrder, cancelOrder, tempLeaveOrder, leaveOrder } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const orders = ref([])
const loading = ref(false)
let timer = null

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await getMyOrders()
    orders.value = res.list || res || []
  } catch (e) {
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadOrders()
  timer = setInterval(loadOrders, 10000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const handleSignIn = async (id) => { 
  try { await signInOrder(id); ElMessage.success('签到成功'); loadOrders() } catch(e) {}
}

const handleCancel = async (id) => { 
  try {
    await ElMessageBox.confirm('确定取消该预约吗？', '提示', { type: 'warning' })
    await cancelOrder(id); ElMessage.success('已取消'); loadOrders() 
  } catch(e) {}
}

const handleTempLeave = async (id) => { 
  try { await tempLeaveOrder(id); ElMessage.success('暂离成功'); loadOrders() } catch(e) {}
}

const handleLeave = async (id) => { 
  try {
    await ElMessageBox.confirm('确定要主动离开吗？', '提示', { type: 'info' })
    await leaveOrder(id); ElMessage.success('已离开'); loadOrders() 
  } catch(e) {}
}

const getStatusTag = (status) => {
  if (status === '待签到') return 'warning'
  if (status === '已签到' || status === '暂离中') return 'success'
  if (status === '已完成') return 'info'
  return 'danger'
}
</script>

<style scoped>
.btn-light-green { background-color: #f0f9eb; color: #67c23a; border-color: #e1f3d8; }
.btn-light-green:hover { background-color: #67c23a; color: #ffffff; border-color: #67c23a; }
.btn-light-blue { background-color: #ecf5ff; color: #409eff; border-color: #d9ecff; }
.btn-light-blue:hover { background-color: #409eff; color: #ffffff; border-color: #409eff; }
.btn-outline-brown { background-color: #fdfbf7; color: #8b5a2b; border-color: #dcb37b; }
.btn-outline-brown:hover { background-color: #8b5a2b; color: #ffffff; border-color: #8b5a2b; }
</style>