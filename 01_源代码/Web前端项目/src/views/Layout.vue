<template>
  <el-container class="layout-container">
    <el-header class="header">
      <div class="logo">校园自习室分布式预约管理系统</div>
      
      <div class="nav-menu">
        <!-- 只有学生能看到这些 -->
        <template v-if="role === 'STUDENT'">
          <router-link to="/student/rooms" class="nav-item">查座预约</router-link>
          <router-link to="/student/orders" class="nav-item">我的订单</router-link>
        </template>
        
        <!-- 只有管理员能看到这些 -->
        <template v-if="role === 'ADMIN'">
          <router-link to="/admin/seats" class="nav-item">座位管理</router-link>
        </template>
      </div>

      <div class="user-info">
        <el-tag type="warning" effect="dark" style="margin-right: 15px;"> {{ role === 'ADMIN' ? '管理员' : '学生' }}</el-tag>
        <el-button type="warning" plain size="small" @click="logout">退出登录</el-button>
      </div>
    </el-header>
    <el-main class="main-content">
      <router-view></router-view>
    </el-main>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ref } from 'vue'

const router = useRouter()
// 从本地缓存读取角色
const role = ref(localStorage.getItem('role') || 'STUDENT')

const logout = () => {
  localStorage.clear()
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  /* 1. 加入你提供的第二张图 */
  background-image: url('https://www.stu.edu.cn/__local/7/AD/61/C140BECFEBEED69EC742BE6F1AE_76D11A52_1A09A.jpg');
  background-size: cover;
  background-position: center;
  background-attachment: fixed; 
  
  /* 2. 用米黄色覆盖，并设置混合模式，让图片变成淡淡的水印底色 */
  background-color: #fdfbf7; 
  background-blend-mode: overlay; 
}

.header { 
  background-color: #8b5a2b; 
  box-shadow: 0 2px 8px rgba(0,0,0,0.15); 
  display: flex; 
  align-items: center; 
  justify-content: space-between; 
  padding: 0 30px; 
  height: 60px; 
}

.logo { 
  font-size: 20px; 
  font-weight: bold; 
  color: #ffd700; 
}

.nav-menu { 
  display: flex; 
  gap: 20px; 
}

.nav-item { 
  text-decoration: none; 
  color: #f5f5dc; 
  font-size: 15px; 
  padding: 10px 15px; 
  border-radius: 4px; 
  transition: all 0.3s; 
}

.nav-item:hover, .nav-item.router-link-active { 
  background-color: #a0522d; 
  color: #fff; 
}

.user-info { 
  display: flex; 
  align-items: center; 
}

.main-content { 
  padding: 20px; 
  max-width: 1200px; 
  margin: 0 auto; 
  width: 100%; 
}
</style>