<script setup>
import { RouterLink } from 'vue-router'

const props = defineProps({
  post: {
    type: Object,
    required: true
  }
})

function formatDate(dateString) {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

function readTime(content) {
  const words = content?.split(/\s+/).length || 0
  const minutes = Math.ceil(words / 200)
  return `${minutes} min read`
}
</script>

<template>
  <article class="card p-6 hover:shadow-xl transition-shadow duration-300 animate-fadeIn">
    <RouterLink :to="`/post/${post.slug}`" class="block">
      <h2 class="text-xl md:text-2xl font-bold text-dark-900 dark:text-white mb-2 hover:text-primary-600 dark:hover:text-primary-400 transition-colors">
        {{ post.title }}
      </h2>
      <p class="text-dark-600 dark:text-dark-400 mb-4 line-clamp-2">
        {{ post.excerpt }}
      </p>
    </RouterLink>
    
    <div class="flex items-center justify-between text-sm text-dark-500 dark:text-dark-400">
      <div class="flex items-center space-x-3">
        <div class="w-8 h-8 bg-primary-500 rounded-full flex items-center justify-center text-white font-medium">
          {{ post.author?.username?.charAt(0).toUpperCase() }}
        </div>
        <div>
          <span class="font-medium text-dark-700 dark:text-dark-300">{{ post.author?.username }}</span>
          <span class="mx-1">·</span>
          <span>{{ formatDate(post.publishedAt) }}</span>
        </div>
      </div>
      <div class="flex items-center space-x-4">
        <span>{{ readTime(post.excerpt) }}</span>
        <span class="flex items-center space-x-1">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <span>{{ post.commentCount }}</span>
        </span>
      </div>
    </div>
  </article>
</template>
