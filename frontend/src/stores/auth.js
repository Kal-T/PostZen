import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../services/api'

export const useAuthStore = defineStore('auth', () => {
    const user = ref(null)
    const accessToken = ref(null)
    const refreshToken = ref(null)
    const initialized = ref(false)

    const isAuthenticated = computed(() => !!accessToken.value)
    const isAdmin = computed(() => user.value?.role === 'ADMIN')

    async function init() {
        const storedToken = localStorage.getItem('accessToken')
        const storedRefresh = localStorage.getItem('refreshToken')
        const storedUser = localStorage.getItem('user')

        if (storedToken && storedUser) {
            accessToken.value = storedToken
            refreshToken.value = storedRefresh
            user.value = JSON.parse(storedUser)
            api.setAuthToken(storedToken)
        }

        initialized.value = true
    }

    async function login(email, password) {
        const response = await api.login({ email, password })
        setAuthData(response)
        return response
    }

    async function register(email, username, password) {
        const response = await api.register({ email, username, password })
        setAuthData(response)
        return response
    }

    async function logout() {
        try {
            await api.logout(refreshToken.value)
        } catch (e) {
            // Ignore logout errors
        }
        clearAuthData()
    }

    async function refreshAccessToken() {
        if (!refreshToken.value) {
            clearAuthData()
            throw new Error('No refresh token')
        }

        try {
            const response = await api.refreshToken(refreshToken.value)
            setAuthData(response)
            return response.accessToken
        } catch (e) {
            clearAuthData()
            throw e
        }
    }

    function setAuthData(response) {
        accessToken.value = response.accessToken
        refreshToken.value = response.refreshToken
        user.value = response.user

        localStorage.setItem('accessToken', response.accessToken)
        localStorage.setItem('refreshToken', response.refreshToken)
        localStorage.setItem('user', JSON.stringify(response.user))

        api.setAuthToken(response.accessToken)
    }

    function clearAuthData() {
        accessToken.value = null
        refreshToken.value = null
        user.value = null

        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('user')

        api.setAuthToken(null)
    }

    return {
        user,
        accessToken,
        refreshToken,
        initialized,
        isAuthenticated,
        isAdmin,
        init,
        login,
        register,
        logout,
        refreshAccessToken
    }
})
