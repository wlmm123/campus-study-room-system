<template>
  <div class="login-container">
    
    <!-- 卡片外面的超大标题 -->
    <h1 class="main-title">校园自习室分布式预约管理系统</h1>

    <!-- 登录卡片 -->
    <el-card class="login-card">
      <div class="header-box">
        <!-- 卡片里面的校徽 -->
        <img src="../assets/stu-logo.png" alt="汕头大学" class="logo-img" />
      </div>

      <el-form label-width="60px" class="login-form">
        <el-form-item label="账号">
          <el-input v-model="username" placeholder="请输入学生/管理员账号" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" placeholder="请输入密码" show-password size="large" @keyup.enter="handleLogin" />
        </el-form-item>
        
        <el-button type="warning" class="login-btn" @click="handleLogin" :loading="loading">
          登 录
        </el-button>
      </el-form>
    </el-card>
    
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { login } from '../api'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const username = ref('20230001')
const password = ref('Password123!')
const loading = ref(false)
const router = useRouter()

const handleLogin = async () => {
  if (!username.value || !password.value) {
    return ElMessage.warning('请输入账号和密码')
  }
  loading.value = true
  try {
    const data = await login({ username: username.value, password: password.value })
    localStorage.setItem('token', data.token)
    localStorage.setItem('role', data.role)
    ElMessage.success('登录成功')
    
    if (data.role === 'ADMIN') {
      router.push('/admin/seats')
    } else {
      router.push('/student/rooms')
    }
  } catch (e) {
    // 异常处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  flex-direction: column; 
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-image: url('https://www.stu.edu.cn/__local/6/54/D1/3E402BA8A266710401D742F8C23_B09C95C3_6FBB3.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
}

/* 背景暗化遮罩 */
.login-container::before {
  content: "";
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background-color: rgba(0, 0, 0, 0.45); /* 稍微加深一点，让外面的白字更清晰 */
  z-index: 0;
}

/* ===== 卡片外面的超大标题 ===== */
.main-title {
  z-index: 1;
  margin: 0;
  margin-bottom: 120px; /* 标题和卡片的间距 */
  color: #ffffff; /* 纯白字 */
  font-weight: 900;
  font-size: 50px; /* 超大号字体 */
  text-align: center;
  letter-spacing: 3px;
  text-shadow: 2px 4px 10px rgba(0, 0, 0, 0.8); /* 强烈的文字阴影，极具立体感 */
}

/* ===== 登录卡片 ===== */
.login-card {
  width: 380px; 
  padding: 20px 35px 35px 35px;
  border-radius: 12px;
  z-index: 1;
  background-color: #ffffff; 
  backdrop-filter: blur(10px);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.4);
  border-top: 4px solid #8b5a2b;
}

/* 卡片内部头部（只有校徽） */
.header-box {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

/* 卡片内的校徽 */
.logo-img {
  width: 160px; 
  height: auto;
}

.login-btn {
  width: 100%; 
  margin-top: 20px; 
  font-size: 16px; 
  letter-spacing: 5px;
  background-color: #8b5a2b;
  border-color: #8b5a2b;
  color: white;
}

.login-btn:hover {
  background-color: #a0522d;
  border-color: #a0522d;
}
</style>