<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import api from '../services/api'
import PostCard from '../components/PostCard.vue'

const authStore = useAuthStore()

const posts = ref([])
const loading = ref(true)
const page = ref(0)
const hasMore = ref(true)

const user = computed(() => authStore.user)

async function loadPosts(reset = false) {
  if (reset) {
    page.value = 0
    posts.value = []
    hasMore.value = true
  }
  
  if (!user.value?.id || !hasMore.value) return
  
  try {
    loading.value = true
    const response = await api.getPostsByAuthor(user.value.id, page.value, 10)
    posts.value = [...posts.value, ...response.content]
    hasMore.value = !response.last
    page.value++
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPosts()
})
</script>

<template>
  <div class="max-w-4xl mx-auto px-4 py-12">
    <!-- Profile Header -->
    <div class="card p-8 mb-8">
      <div class="flex items-center space-x-4">
        <div class="w-20 h-20 bg-primary-500 rounded-full flex items-center justify-center text-white text-3xl font-bold">
          {{ user?.username?.charAt(0).toUpperCase() }}
        </div>
        <div>
          <h1 class="text-2xl font-bold text-dark-900 dark:text-white">{{ user?.username }}</h1>
          <p class="text-dark-500 dark:text-dark-400">{{ user?.email }}</p>
          <span class="inline-block mt-2 px-2 py-1 text-xs font-medium rounded-full bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300">
            {{ user?.role }}
          </span>
        </div>
      </div>
    </div>
    
    <!-- User's Posts -->
    <h2 class="text-xl font-bold text-dark-900 dark:text-white mb-6">Your Stories</h2>
    
    <div v-if="loading && posts.length === 0" class="space-y-4">
      <div v-for="i in 3" :key="i" class="card p-6 animate-pulse">
        <div class="h-6 bg-dark-200 dark:bg-dark-700 rounded w-3/4 mb-4"></div>
        <div class="h-4 bg-dark-200 dark:bg-dark-700 rounded w-full"></div>
      </div>
    </div>
    
    <div v-else-if="posts.length === 0" class="card p-8 text-center">
      <div class="text-6xl mb-4">✍️</div>
      <h3 class="text-xl font-bold text-dark-900 dark:text-white mb-2">No stories yet</h3>
      <p class="text-dark-500 dark:text-dark-400 mb-4">Start sharing your thoughts with the world!</p>
      <RouterLink to="/write" class="btn-primary">
        Write Your First Story
      </RouterLink>
    </div>
    
    <div v-else class="space-y-6">
      <PostCard v-for="post in posts" :key="post.id" :post="post" />
      
      <button 
        v-if="hasMore"
        @click="loadPosts()"
        :disabled="loading"
        class="btn-secondary w-full"
      >
        {{ loading ? 'Loading...' : 'Load More' }}
      </button>
    </div>
  </div>
</template>
