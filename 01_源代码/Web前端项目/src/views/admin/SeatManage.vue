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
            <!-- 修复 BUG-006：使用下拉框切换状态，可以随时恢复“可用” -->
            <el-select 
              v-model="row.status" 
              size="small" 
              @change="(val) => handleStatusChange(row, val)"
              :disabled="['已占用', '暂离中'].includes(row.status)"
            >
              <el-option label="可用" value="可用" />
              <el-option label="维护" value="维护" />
              <el-option label="停用" value="停用" />
            </el-select>
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
import dayjs from 'dayjs' // 用于生成时间

const searchRoomId = ref(1)
const seats = ref([])
const loading = ref(false)

const loadSeats = async () => {
  loading.value = true
  try {
    // 【修复 BUG-005】带上时间参数，防止后端返回 400，但表格不显示这列
    const startTime = dayjs().format('YYYY-MM-DD HH:mm:ss')
    const endTime = dayjs().add(2, 'hour').format('YYYY-MM-DD HH:mm:ss')
    
    const res = await getSeats(searchRoomId.value, { startTime, endTime }) 
    seats.value = res.list || res || []
  } catch (e) {
    ElMessage.error('查询座位失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => loadSeats())

// 【修复 BUG-006】处理状态切换（包含强制维护和恢复可用）
const handleStatusChange = async (seat, newStatus) => {
  if (newStatus === '可用') {
    // 恢复可用直接发请求
    try {
      await setSeatStatus(seat.seatId, { status: '可用' })
      ElMessage.success('已恢复为可用状态')
      loadSeats()
    } catch (err) {
      ElMessage.error('恢复失败')
    }
    return
  }

  // 设为维护/停用时的 1006 拦截逻辑
  try {
    await setSeatStatus(seat.seatId, { status: newStatus })
    ElMessage.success(`座位已设为${newStatus}`)
    loadSeats()
  } catch (err) {
    if (err.code === 1006) {
      const affected = err.data?.affectedOrders || []
      if (affected.length === 0) {
        ElMessage.error('座位正在使用中，无法设置维护/停用')
        return loadSeats() // 失败后刷新，恢复原本状态
      }
      
      const msg = `该座位有 ${affected.length} 个未来预约，是否强制取消并设为${newStatus}？`
      try {
        await ElMessageBox.confirm(msg, '确认强制操作', { confirmButtonText: '强制取消并操作', cancelButtonText: '取消', type: 'warning' })
        loading.value = true
        for (let order of affected) {
          await cancelOrder(order.orderId)
        }
        await setSeatStatus(seat.seatId, { status: newStatus })
        ElMessage.success(`已强制设为${newStatus}`)
        loadSeats()
      } catch (cancelError) {
        if (cancelError !== 'cancel') ElMessage.error('操作失败')
        loadSeats() // 用户取消，刷新页面恢复状态
      } finally {
        loading.value = false
      }
    } else {
      loadSeats()
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