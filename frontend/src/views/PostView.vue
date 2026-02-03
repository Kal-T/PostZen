<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import api from '../services/api'
import { useAuthStore } from '../stores/auth'
import CommentSection from '../components/CommentSection.vue'

const route = useRoute()
const authStore = useAuthStore()

const post = ref(null)
const loading = ref(true)
const error = ref(null)

const isAuthenticated = computed(() => authStore.isAuthenticated)
const isAuthor = computed(() => {
  return authStore.user?.id === post.value?.author?.id
})

const renderedContent = computed(() => {
  if (!post.value?.content) return ''
  const html = marked(post.value.content)
  return DOMPurify.sanitize(html)
})

function formatDate(dateString) {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

function readTime(content) {
  const words = content?.split(/\s+/).length || 0
  const minutes = Math.ceil(words / 200)
  return `${minutes} min read`
}

async function loadPost() {
  try {
    loading.value = true
    post.value = await api.getPost(route.params.slug)
    error.value = null
  } catch (e) {
    error.value = 'Post not found'
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPost()
})
</script>

<template>
  <div class="max-w-4xl mx-auto px-4 py-12">
    <!-- Loading -->
    <div v-if="loading" class="animate-pulse">
      <div class="h-10 bg-dark-200 dark:bg-dark-700 rounded w-3/4 mb-4"></div>
      <div class="h-4 bg-dark-200 dark:bg-dark-700 rounded w-1/4 mb-8"></div>
      <div class="space-y-3">
        <div class="h-4 bg-dark-200 dark:bg-dark-700 rounded w-full"></div>
        <div class="h-4 bg-dark-200 dark:bg-dark-700 rounded w-full"></div>
        <div class="h-4 bg-dark-200 dark:bg-dark-700 rounded w-2/3"></div>
      </div>
    </div>
    
    <!-- Error -->
    <div v-else-if="error" class="text-center py-12">
      <div class="text-6xl mb-4">😕</div>
      <h1 class="text-2xl font-bold text-dark-900 dark:text-white mb-2">{{ error }}</h1>
      <RouterLink to="/" class="btn-primary mt-4 inline-block">
        Back to Home
      </RouterLink>
    </div>
    
    <!-- Post Content -->
    <article v-else>
      <header class="mb-8">
        <h1 class="text-3xl md:text-4xl lg:text-5xl font-bold text-dark-900 dark:text-white mb-4">
          {{ post.title }}
        </h1>
        
        <div class="flex items-center justify-between flex-wrap gap-4">
          <div class="flex items-center space-x-3">
            <div class="w-12 h-12 bg-primary-500 rounded-full flex items-center justify-center text-white text-xl font-medium">
              {{ post.author?.username?.charAt(0).toUpperCase() }}
            </div>
            <div>
              <div class="font-medium text-dark-900 dark:text-white">{{ post.author?.username }}</div>
              <div class="text-sm text-dark-500 dark:text-dark-400">
                {{ formatDate(post.publishedAt) }} · {{ readTime(post.content) }}
              </div>
            </div>
          </div>
          
          <div v-if="isAuthor" class="flex space-x-2">
            <RouterLink :to="`/edit/${post.id}`" class="btn-secondary">
              Edit
            </RouterLink>
          </div>
        </div>
      </header>
      
      <div class="prose dark:prose-dark" v-html="renderedContent"></div>
      
      <hr class="my-12 border-dark-200 dark:border-dark-700" />
      
      <!-- Comments Section -->
      <CommentSection v-if="post.id" :post-id="post.id" />
    </article>
  </div>
</template>
