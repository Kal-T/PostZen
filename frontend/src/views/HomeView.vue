<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'
import api from '../services/api'
import PostCard from '../components/PostCard.vue'

const posts = ref([])
const loading = ref(true)
const loadingMore = ref(false)
const hasMore = ref(true)
const page = ref(0)
const error = ref(null)

async function loadPosts(reset = false) {
  if (reset) {
    page.value = 0
    posts.value = []
    hasMore.value = true
  }
  
  if (!hasMore.value || loadingMore.value) return
  
  try {
    if (page.value === 0) {
      loading.value = true
    } else {
      loadingMore.value = true
    }
    
    const response = await api.getPosts(page.value, 10)
    posts.value = [...posts.value, ...response.content]
    hasMore.value = !response.last
    page.value++
    error.value = null
  } catch (e) {
    error.value = 'Failed to load posts'
    console.error(e)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function handleScroll() {
  const scrollTop = window.scrollY
  const windowHeight = window.innerHeight
  const documentHeight = document.documentElement.scrollHeight
  
  if (scrollTop + windowHeight >= documentHeight - 500 && hasMore.value && !loadingMore.value) {
    loadPosts()
  }
}

onMounted(() => {
  loadPosts()
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<template>
  <div>
    <!-- Hero Section -->
    <section class="bg-gradient-to-br from-primary-50 to-primary-100 dark:from-dark-900 dark:to-dark-800 py-20">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h1 class="text-4xl md:text-5xl lg:text-6xl font-bold text-dark-900 dark:text-white mb-6">
          Share Your <span class="text-primary-600 dark:text-primary-400">Stories</span>
        </h1>
        <p class="text-lg md:text-xl text-dark-600 dark:text-dark-300 max-w-2xl mx-auto mb-8">
          A place for thoughtful readers and curious minds. Discover stories, thinking, and expertise from writers on any topic.
        </p>
        <div class="flex flex-col sm:flex-row gap-4 justify-center">
          <RouterLink to="/register" class="btn-primary text-lg px-8 py-3">
            Start Writing
          </RouterLink>
          <RouterLink to="/" class="btn-secondary text-lg px-8 py-3">
            Explore Stories
          </RouterLink>
        </div>
      </div>
    </section>

    <!-- Posts Feed -->
    <section class="max-w-4xl mx-auto px-4 py-12">
      <h2 class="text-2xl font-bold text-dark-900 dark:text-white mb-8">Latest Stories</h2>
      
      <!-- Loading State -->
      <div v-if="loading" class="space-y-6">
        <div v-for="i in 3" :key="i" class="card p-6 animate-pulse">
          <div class="h-6 bg-dark-200 dark:bg-dark-700 rounded w-3/4 mb-4"></div>
          <div class="h-4 bg-dark-200 dark:bg-dark-700 rounded w-full mb-2"></div>
          <div class="h-4 bg-dark-200 dark:bg-dark-700 rounded w-2/3"></div>
        </div>
      </div>
      
      <!-- Error State -->
      <div v-else-if="error" class="card p-8 text-center">
        <p class="text-red-500 mb-4">{{ error }}</p>
        <button @click="loadPosts(true)" class="btn-primary">
          Try Again
        </button>
      </div>
      
      <!-- Empty State -->
      <div v-else-if="posts.length === 0" class="card p-8 text-center">
        <div class="text-6xl mb-4">📝</div>
        <h3 class="text-xl font-bold text-dark-900 dark:text-white mb-2">No stories yet</h3>
        <p class="text-dark-500 dark:text-dark-400 mb-4">Be the first to share your story!</p>
        <RouterLink to="/write" class="btn-primary">
          Write a Story
        </RouterLink>
      </div>
      
      <!-- Posts List -->
      <div v-else class="space-y-6">
        <PostCard v-for="post in posts" :key="post.id" :post="post" />
        
        <!-- Loading More -->
        <div v-if="loadingMore" class="flex justify-center py-4">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>
        
        <!-- End of List -->
        <div v-if="!hasMore && posts.length > 0" class="text-center py-8 text-dark-500 dark:text-dark-400">
          You've reached the end! 🎉
        </div>
      </div>
    </section>
  </div>
</template>
