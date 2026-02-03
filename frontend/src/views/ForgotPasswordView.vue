<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import api from '../services/api'

const email = ref('')
const loading = ref(false)
const success = ref(false)
const error = ref(null)

async function handleSubmit() {
  if (!email.value) {
    error.value = 'Please enter your email'
    return
  }
  
  try {
    loading.value = true
    error.value = null
    await api.forgotPassword(email.value)
    success.value = true
  } catch (e) {
    error.value = e.response?.data?.message || 'Something went wrong'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-[calc(100vh-200px)] flex items-center justify-center py-12 px-4">
    <div class="card max-w-md w-full p-8">
      <div class="text-center mb-8">
        <h1 class="text-2xl font-bold text-dark-900 dark:text-white mb-2">Reset Password</h1>
        <p class="text-dark-500 dark:text-dark-400">Enter your email to receive a reset link</p>
      </div>
      
      <div v-if="success" class="text-center">
        <div class="text-6xl mb-4">📧</div>
        <p class="text-dark-600 dark:text-dark-400 mb-4">
          If an account exists with that email, we've sent a password reset link.
        </p>
        <RouterLink to="/login" class="btn-primary inline-block">
          Back to Login
        </RouterLink>
      </div>
      
      <form v-else @submit.prevent="handleSubmit" class="space-y-4">
        <div>
          <label for="email" class="block text-sm font-medium text-dark-700 dark:text-dark-300 mb-1">
            Email
          </label>
          <input
            id="email"
            v-model="email"
            type="email"
            class="input"
            placeholder="you@example.com"
            required
          />
        </div>
        
        <div v-if="error" class="p-3 bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-lg text-red-600 dark:text-red-400 text-sm">
          {{ error }}
        </div>
        
        <button
          type="submit"
          :disabled="loading"
          class="btn-primary w-full py-3"
        >
          {{ loading ? 'Sending...' : 'Send Reset Link' }}
        </button>
      </form>
      
      <div v-if="!success" class="mt-6 text-center text-sm text-dark-500 dark:text-dark-400">
        Remember your password?
        <RouterLink to="/login" class="link ml-1">
          Sign in
        </RouterLink>
      </div>
    </div>
  </div>
</template>
