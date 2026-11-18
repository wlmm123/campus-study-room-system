<template>
  <div class="room-list">
    <el-card class="filter-card" style="margin-bottom: 20px;">
      <h3 style="color: #8b5a2b;">1. 选择使用时段</h3>
      <div style="display: flex; gap: 15px; align-items: center; margin-top: 15px;">
        <el-date-picker v-model="queryForm.date" type="date" placeholder="选择日期" :disabled-date="disabledDate" />
        <el-time-picker v-model="queryForm.time" placeholder="选择开始时间" format="HH:mm" value-format="HH:mm" />
        <el-select v-model="queryForm.durationHours" placeholder="选择时长(档位)" style="width: 120px;">
          <el-option v-for="h in availableDurations" :key="h" :label="`${h} 档`" :value="h" />
        </el-select>
        <el-button type="warning" @click="loadSeats" style="background-color: #8b5a2b; border-color: #8b5a2b; color: white;">查询可用座位</el-button>
      </div>
    </el-card>

    <el-card v-if="seats.length > 0">
      <h3 style="color: #8b5a2b;">2. 选择座位（绿色为可预约）</h3>
      <div class="seat-grid" style="margin-top: 20px;">
        <div 
          v-for="seat in seats" 
          :key="seat.seatId" 
          class="seat-item"
          :class="{ 'is-bookable': seat.bookable, 'is-disabled': !seat.bookable }"
          @click="handleReserve(seat)"
        >
          <div class="seat-name" style="font-weight: bold; font-size: 16px;">{{ seat.seatNumber || seat.seatId }}</div>
          <div class="seat-status" style="font-size: 12px; margin-top: 5px;">{{ seat.status }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { getSeats, reserveSeat } from '../../api'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const queryForm = ref({ date: new Date(), time: dayjs().format('HH:mm'), durationHours: 2 })
const seats = ref([])
const currentRoomId = ref(1)

const availableDurations = computed(() => {
  if (!queryForm.value.time) return []
  const start = dayjs(`${dayjs(queryForm.value.date).format('YYYY-MM-DD')} ${queryForm.value.time}`)
  const nextDayMidnight = start.add(1, 'day').startOf('day')
  const maxHours = nextDayMidnight.diff(start, 'hour')
  const configDurations = [1, 2, 3, 4, 5, 6, 7, 8]
  return configDurations.filter(h => h <= maxHours)
})

const disabledDate = (time) => time.getTime() < Date.now() - 8.64e7

const loadSeats = async () => {
  if (!queryForm.value.time || !queryForm.value.durationHours) return ElMessage.warning('请选择完整时间')
  const dateStr = dayjs(queryForm.value.date).format('YYYY-MM-DD')
  const startTime = dayjs(`${dateStr} ${queryForm.value.time}`).format('YYYY-MM-DD HH:mm:ss')
  const endTime = dayjs(`${dateStr} ${queryForm.value.time}`).add(queryForm.value.durationHours, 'hour').format('YYYY-MM-DD HH:mm:ss')
  
  try {
    const res = await getSeats(currentRoomId.value, { startTime, endTime })
    seats.value = res.list || res || []
  } catch (e) {
    ElMessage.error('获取座位失败')
  }
}

const handleReserve = async (seat) => {
  if (!seat.bookable) return ElMessage.error('该时段已被占用，不可预约')
  
  const finalSeatId = seat.seatId;
  if (!finalSeatId) return ElMessage.error('未获取到座位ID，请检查接口字段')
  
  const dateStr = dayjs(queryForm.value.date).format('YYYY-MM-DD')
  const startTime = dayjs(`${dateStr} ${queryForm.value.time}`).format('YYYY-MM-DD HH:mm:ss')
  
  try {
    await reserveSeat({ 
      seatId: finalSeatId, 
      startTime: startTime, 
      durationHours: queryForm.value.durationHours 
    })
    ElMessage.success('预约成功！')
    loadSeats()
  } catch (e) {}
}
</script>

<style scoped>
.seat-grid { display: flex; flex-wrap: wrap; gap: 15px; }
.seat-item { width: 90px; height: 90px; border: 1px solid #dcdfe6; border-radius: 8px; display: flex; flex-direction: column; align-items: center; justify-content: center; transition: all 0.3s; }
.is-bookable { background: #f0f9eb; border-color: #67c23a; cursor: pointer; color: #67c23a; }
.is-bookable:hover { transform: scale(1.05); box-shadow: 0 2px 12px 0 rgba(0,0,0,.1); }
.is-disabled { background: #f4f4f5; color: #909399; cursor: not-allowed; }
</style>