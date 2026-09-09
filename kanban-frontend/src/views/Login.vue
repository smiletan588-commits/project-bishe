<template>
  <div class="login-page">
    <section class="login-layout">
      <div class="brand-side">
        <BrandMark />
        <div class="brand-copy">
          <p>轻量级项目管理，<br>让你的团队协作井然有序</p>
        </div>
      </div>

      <div class="form-side">
        <div class="form-wrapper" aria-live="polite">
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
              登录
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
              注册
            </el-button>
          </template>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import BrandMark from '@/components/BrandMark.vue'

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
  min-height: 100dvh;
  padding: clamp(18px, 4vw, 52px);
  background: var(--bg-base);
}
.login-layout {
  display: grid;
  grid-template-columns: minmax(320px, .85fr) minmax(460px, 1.15fr);
  width: min(1120px, 100%);
  min-height: calc(100dvh - clamp(36px, 8vw, 104px));
  margin: 0 auto;
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-sm);
}
.brand-side {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: clamp(28px, 5vw, 62px);
  border-right: 1px solid var(--border-light);
  background: var(--brand-soft);
}
.brand-copy { max-width: 420px; padding-bottom: 8vh; }
.brand-copy p { margin: 0; color: var(--text-primary); font-size: clamp(30px, 4vw, 52px); font-weight: 720; line-height: 1.12; letter-spacing: -.045em; }
.form-side {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(36px, 8vw, 96px);
  background: var(--surface);
}
.form-wrapper { width: min(380px, 100%); }
.form-wrapper h2 { margin: 0; color: var(--text-primary); font-size: 28px; line-height: 1.2; letter-spacing: -.035em; }
.subtitle { margin: 9px 0 0; color: var(--text-tertiary); font-size: 13px; }
.auth-tabs { margin: 26px 0 12px; }
.auth-tabs :deep(.el-tabs__header) { margin-bottom: 14px; }
.auth-tabs :deep(.el-tabs__item) { height: 40px; padding: 0 22px 0 0; font-weight: 620; }
.input-group { margin-bottom: 17px; }
.submit-btn { width: 100%; height: 44px; margin-top: 5px; }

@media (max-width: 768px) {
  .login-page { padding: 0; }
  .login-layout { grid-template-columns: 1fr; min-height: 100dvh; border: 0; border-radius: 0; }
  .brand-side { min-height: 190px; padding: 26px 24px 30px; border-right: 0; border-bottom: 1px solid var(--border-light); }
  .brand-copy { padding: 34px 0 0; }
  .brand-copy p { max-width: 13ch; font-size: 27px; }
  .form-side { align-items: flex-start; padding: 38px 24px 48px; }
}
</style>
