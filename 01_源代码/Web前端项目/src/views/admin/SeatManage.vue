<template>
  <div class="seat-manage">
    <h2 style="color: #8b5a2b;">座位管理与强制维护</h2>
    <el-card style="margin-top: 20px;">
      <div style="margin-bottom: 20px; display: flex; gap: 10px;">
        <el-input v-model="searchRoomId" placeholder="输入自习室ID查询" style="width: 200px;" />
        <el-button type="warning" @click="loadSeats" style="background-color: #8b5a2b; border-color: #8b5a2b; color: white;">查询座位</el-button>
      </div>

      <el-table :data="seats" border v-loading="loading">
        <el-table-column prop="seatId" label="座位ID" width="100" />
        <el-table-column prop="seatNumber" label="座位号" width="120" />
        <el-table-column prop="status" label="当前状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button 
              size="small" 
              type="warning" 
              :disabled="['已占用', '暂离中'].includes(row.status)"
              @click="handleSetMaintenance(row.seatId)"
            >
              设为维护
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getSeats, setSeatStatus, cancelOrder } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const searchRoomId = ref(1)
const seats = ref([])
const loading = ref(false)

const loadSeats = async () => {
  loading.value = true
  try {
    const res = await getSeats(searchRoomId.value, {}) 
    seats.value = res.list || res || []
  } catch (e) {
    ElMessage.error('查询座位失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => loadSeats())

const handleSetMaintenance = async (seatId) => {
  try {
    await setSeatStatus(seatId, { status: '维护' })
    ElMessage.success(`座位已设为维护`)
    loadSeats()
  } catch (err) {
    if (err.code === 1006) {
      const affected = err.data?.affectedOrders || []
      if (affected.length === 0) return ElMessage.error('存在未知冲突')
      
      const msg = `该座位有 ${affected.length} 个未来预约，是否强制取消并设为维护？`
      try {
        await ElMessageBox.confirm(msg, '确认强制维护', { confirmButtonText: '强制取消并维护', cancelButtonText: '取消操作', type: 'warning' })
        loading.value = true
        for (let order of affected) {
          await cancelOrder(order.orderId)
        }
        await setSeatStatus(seatId, { status: '维护' })
        ElMessage.success('已强制维护')
        loadSeats()
      } catch (cancelError) {
        if (cancelError !== 'cancel') ElMessage.error('强制维护失败')
      } finally {
        loading.value = false
      }
    }
  }
}

const getStatusTag = (status) => {
  if (status === '可用') return 'success'
  if (status === '已预约') return 'warning'
  if (status === '已占用') return 'danger'
  if (status === '暂离中') return 'info'
  if (status === '维护' || status === '停用') return 'danger'
  return ''
}
</script>