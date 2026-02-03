<script setup>
import { RouterView } from 'vue-router'
import NavBar from './components/NavBar.vue'
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <NavBar />
    <main class="flex-1">
      <RouterView />
    </main>
    <footer class="bg-dark-100 dark:bg-dark-900 py-6 mt-auto">
      <div class="max-w-7xl mx-auto px-4 text-center text-dark-500 dark:text-dark-400">
        <p>&copy; 2024 PostZen. Share your stories with the world.</p>
      </div>
    </footer>
  </div>
</template>
