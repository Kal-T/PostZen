<script setup>
import { ref, computed } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const router = useRouter()
const mobileMenuOpen = ref(false)
const userMenuOpen = ref(false)

const isAuthenticated = computed(() => authStore.isAuthenticated)
const user = computed(() => authStore.user)

async function handleLogout() {
  await authStore.logout()
  userMenuOpen.value = false
  router.push('/')
}
</script>

<template>
  <nav class="bg-white dark:bg-dark-900 shadow-sm border-b border-dark-100 dark:border-dark-800 sticky top-0 z-50">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex justify-between h-16">
        <!-- Logo -->
        <div class="flex items-center">
          <RouterLink to="/" class="flex items-center space-x-2">
            <div class="w-8 h-8 bg-gradient-to-br from-primary-500 to-primary-700 rounded-lg flex items-center justify-center">
              <span class="text-white font-bold text-lg">P</span>
            </div>
            <span class="text-xl font-bold text-dark-900 dark:text-white">PostZen</span>
          </RouterLink>
        </div>

        <!-- Desktop Navigation -->
        <div class="hidden md:flex items-center space-x-4">
          <RouterLink to="/" class="px-3 py-2 text-dark-600 dark:text-dark-300 hover:text-primary-600 dark:hover:text-primary-400 transition-colors">
            Home
          </RouterLink>
          
          <template v-if="isAuthenticated">
            <RouterLink to="/write" class="btn-primary">
              Write
            </RouterLink>
            
            <div class="relative">
              <button 
                @click="userMenuOpen = !userMenuOpen"
                class="flex items-center space-x-2 px-3 py-2 rounded-lg hover:bg-dark-100 dark:hover:bg-dark-800 transition-colors"
              >
                <div class="w-8 h-8 bg-primary-500 rounded-full flex items-center justify-center text-white font-medium">
                  {{ user?.username?.charAt(0).toUpperCase() }}
                </div>
                <span class="text-dark-700 dark:text-dark-300">{{ user?.username }}</span>
                <svg class="w-4 h-4 text-dark-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                </svg>
              </button>
              
              <div 
                v-if="userMenuOpen"
                class="absolute right-0 mt-2 w-48 bg-white dark:bg-dark-800 rounded-lg shadow-lg border border-dark-100 dark:border-dark-700 py-1"
              >
                <RouterLink 
                  to="/profile"
                  @click="userMenuOpen = false"
                  class="block px-4 py-2 text-dark-700 dark:text-dark-300 hover:bg-dark-50 dark:hover:bg-dark-700"
                >
                  Profile
                </RouterLink>
                <button 
                  @click="handleLogout"
                  class="w-full text-left px-4 py-2 text-red-600 hover:bg-dark-50 dark:hover:bg-dark-700"
                >
                  Logout
                </button>
              </div>
            </div>
          </template>
          
          <template v-else>
            <RouterLink to="/login" class="btn-secondary">
              Login
            </RouterLink>
            <RouterLink to="/register" class="btn-primary">
              Get Started
            </RouterLink>
          </template>
        </div>

        <!-- Mobile menu button -->
        <div class="md:hidden flex items-center">
          <button 
            @click="mobileMenuOpen = !mobileMenuOpen"
            class="p-2 rounded-lg text-dark-500 hover:bg-dark-100 dark:hover:bg-dark-800"
          >
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path v-if="!mobileMenuOpen" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
              <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Mobile menu -->
    <div v-if="mobileMenuOpen" class="md:hidden border-t border-dark-100 dark:border-dark-800">
      <div class="px-4 py-3 space-y-2">
        <RouterLink to="/" @click="mobileMenuOpen = false" class="block px-3 py-2 rounded-lg hover:bg-dark-100 dark:hover:bg-dark-800">
          Home
        </RouterLink>
        
        <template v-if="isAuthenticated">
          <RouterLink to="/write" @click="mobileMenuOpen = false" class="block px-3 py-2 rounded-lg hover:bg-dark-100 dark:hover:bg-dark-800">
            Write
          </RouterLink>
          <RouterLink to="/profile" @click="mobileMenuOpen = false" class="block px-3 py-2 rounded-lg hover:bg-dark-100 dark:hover:bg-dark-800">
            Profile
          </RouterLink>
          <button @click="handleLogout" class="w-full text-left px-3 py-2 text-red-600 rounded-lg hover:bg-dark-100 dark:hover:bg-dark-800">
            Logout
          </button>
        </template>
        
        <template v-else>
          <RouterLink to="/login" @click="mobileMenuOpen = false" class="block px-3 py-2 rounded-lg hover:bg-dark-100 dark:hover:bg-dark-800">
            Login
          </RouterLink>
          <RouterLink to="/register" @click="mobileMenuOpen = false" class="block px-3 py-2 rounded-lg bg-primary-600 text-white text-center">
            Get Started
          </RouterLink>
        </template>
      </div>
    </div>
  </nav>
</template>
