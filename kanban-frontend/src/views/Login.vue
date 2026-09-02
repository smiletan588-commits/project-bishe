<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="brand-side">
        <div class="brand-content">
          <div class="logo-mark">
            <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
              <rect width="40" height="40" rx="10" fill="rgba(255,255,255,0.15)"/>
              <rect x="8" y="10" width="10" height="8" rx="2" fill="white" opacity="0.9"/>
              <rect x="22" y="10" width="10" height="8" rx="2" fill="white" opacity="0.7"/>
              <rect x="8" y="22" width="10" height="8" rx="2" fill="white" opacity="0.6"/>
              <rect x="22" y="22" width="10" height="8" rx="2" fill="white" opacity="0.8"/>
            </svg>
          </div>
          <h1>SmartPM</h1>
          <p>轻量级项目管理，让你的团队协作井然有序</p>
        </div>
      </div>

      <div class="form-side">
        <div class="form-wrapper">
          <h2>{{ activeTab === 'login' ? '欢迎回来' : '创建账户' }}</h2>
          <p class="subtitle">
            {{ activeTab === 'login' ? '登录以继续你的工作' : '注册后即可创建和管理项目' }}
          </p>

          <el-tabs v-model="activeTab" class="auth-tabs">
            <el-tab-pane label="登录" name="login" />
            <el-tab-pane label="注册" name="register" />
          </el-tabs>

          <template v-if="activeTab === 'login'">
            <div class="input-group">
              <label>用户名</label>
              <el-input v-model="loginForm.username" placeholder="输入用户名" size="large" />
            </div>
            <div class="input-group">
              <label>密码</label>
              <el-input v-model="loginForm.password" type="password" placeholder="输入密码" size="large"
                @keyup.enter="handleLogin" />
            </div>
            <el-button type="primary" size="large" :loading="loading" class="submit-btn"
              @click="handleLogin">
              登 录
            </el-button>
          </template>

          <template v-else>
            <div class="input-group">
              <label>用户名</label>
              <el-input v-model="registerForm.username" placeholder="输入用户名" size="large" />
            </div>
            <div class="input-group">
              <label>昵称</label>
              <el-input v-model="registerForm.nickname" placeholder="给自己起个名字" size="large" />
            </div>
            <div class="input-group">
              <label>密码</label>
              <el-input v-model="registerForm.password" type="password" placeholder="设置密码" size="large"
                @keyup.enter="handleRegister" />
            </div>
            <el-button type="primary" size="large" :loading="loading" class="submit-btn"
              @click="handleRegister">
              注 册
            </el-button>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('login')
const loading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', nickname: '' })

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login(loginForm.username, loginForm.password)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.username || !registerForm.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    await userStore.register(registerForm.username, registerForm.password, registerForm.nickname)
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    registerForm.username = ''
    registerForm.password = ''
    registerForm.nickname = ''
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-base);
  padding: 24px;
}
.login-panel {
  display: flex;
  width: 880px;
  min-height: 560px;
  background: var(--bg-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}
.brand-side {
  flex: 0 0 380px;
  background: var(--brand-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}
.brand-content {
  text-align: center;
  color: #fff;
}
.logo-mark {
  margin-bottom: 24px;
}
.brand-content h1 {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  letter-spacing: -0.5px;
}
.brand-content p {
  margin: 12px 0 0;
  font-size: 15px;
  opacity: 0.85;
  line-height: 1.6;
}
.form-side {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
}
.form-wrapper {
  width: 100%;
  max-width: 340px;
}
.form-wrapper h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}
.subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--text-tertiary);
}
.auth-tabs {
  margin: 20px 0 8px;
}
.auth-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}
.input-group {
  margin-bottom: 16px;
}
.input-group label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 6px;
}
.input-group :deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
  box-shadow: 0 0 0 1px var(--border) inset;
}
.input-group :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--brand) inset;
}
.input-group :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(99,102,241,0.25) inset;
}
.submit-btn {
  width: 100%;
  margin-top: 8px;
  border-radius: var(--radius-sm);
  font-weight: 600;
  letter-spacing: 2px;
  height: 44px;
  background: var(--brand-gradient);
  border: none;
}
.submit-btn:hover {
  opacity: 0.92;
}

@media (max-width: 768px) {
  .login-panel {
    flex-direction: column;
    width: 100%;
    min-height: auto;
  }
  .brand-side {
    flex: none;
    padding: 32px 24px;
  }
  .brand-content h1 { font-size: 24px; }
  .form-side { padding: 32px 24px; }
}
</style>
