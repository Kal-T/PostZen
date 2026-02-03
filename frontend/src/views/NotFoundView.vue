<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <div class="min-h-[calc(100vh-200px)] flex items-center justify-center py-12 px-4">
    <div class="text-center">
      <div class="text-8xl mb-6">🔍</div>
      <h1 class="text-4xl font-bold text-dark-900 dark:text-white mb-4">Page Not Found</h1>
      <p class="text-dark-500 dark:text-dark-400 mb-8">
        The page you're looking for doesn't exist or has been moved.
      </p>
      <RouterLink to="/" class="btn-primary">
        Back to Home
      </RouterLink>
    </div>
  </div>
</template>
