import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi, updateIdentity as updateIdentityApi } from '@/api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')
  }),

  getters: {
    isLoggedIn: state => !!state.token && !!state.userInfo,
    identity: state => state.userInfo?.identity || null,
    needsIdentityPrompt: state => !!state.token && !!state.userInfo && !state.userInfo.identity
  },

  actions: {
    async login(username, password) {
      const res = await loginApi(username, password)
      const { token, userId, username: name, identity } = res.data.data
      this.token = token
      this.userInfo = { userId, username: name, identity: identity || null }
      localStorage.setItem('token', token)
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
    },

    async register(username, password, nickname) {
      await registerApi(username, password, nickname)
    },

    async updateIdentity(identity) {
      await updateIdentityApi(identity)
      this.userInfo = { ...this.userInfo, identity }
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
    },

    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})
