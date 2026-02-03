<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../services/api'
import { useAuthStore } from '../stores/auth'

const props = defineProps({
  postId: {
    type: String,
    required: true
  }
})

const authStore = useAuthStore()

const comments = ref([])
const newComment = ref('')
const loading = ref(true)
const submitting = ref(false)
const error = ref(null)
const page = ref(0)
const hasMore = ref(true)

const isAuthenticated = computed(() => authStore.isAuthenticated)
const currentUserId = computed(() => authStore.user?.id)

async function loadComments(reset = false) {
  if (reset) {
    page.value = 0
    comments.value = []
    hasMore.value = true
  }
  
  try {
    loading.value = true
    const response = await api.getComments(props.postId, page.value, 20)
    comments.value = [...comments.value, ...response.content]
    hasMore.value = !response.last
    page.value++
  } catch (e) {
    error.value = 'Failed to load comments'
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function submitComment() {
  if (!newComment.value.trim()) return
  
  try {
    submitting.value = true
    const comment = await api.createComment(props.postId, newComment.value)
    comments.value.unshift(comment)
    newComment.value = ''
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to post comment'
  } finally {
    submitting.value = false
  }
}

async function deleteComment(id) {
  if (!confirm('Delete this comment?')) return
  
  try {
    await api.deleteComment(id)
    comments.value = comments.value.filter(c => c.id !== id)
  } catch (e) {
    error.value = 'Failed to delete comment'
  }
}

function formatDate(dateString) {
  const date = new Date(dateString)
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

onMounted(() => {
  loadComments()
})
</script>

<template>
  <section>
    <h2 class="text-2xl font-bold text-dark-900 dark:text-white mb-6">Comments</h2>
    
    <!-- Comment Form -->
    <div v-if="isAuthenticated" class="mb-8">
      <textarea
        v-model="newComment"
        rows="3"
        placeholder="Write a comment..."
        class="input resize-none"
      ></textarea>
      <div class="flex justify-end mt-2">
        <button 
          @click="submitComment"
          :disabled="submitting || !newComment.trim()"
          class="btn-primary"
        >
          {{ submitting ? 'Posting...' : 'Post Comment' }}
        </button>
      </div>
    </div>
    <div v-else class="mb-8 p-4 bg-dark-50 dark:bg-dark-800 rounded-lg text-center">
      <p class="text-dark-600 dark:text-dark-400">
        <RouterLink to="/login" class="link">Sign in</RouterLink> to leave a comment.
      </p>
    </div>
    
    <!-- Error -->
    <div v-if="error" class="mb-4 p-3 bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-lg text-red-600 dark:text-red-400 text-sm">
      {{ error }}
    </div>
    
    <!-- Comments List -->
    <div v-if="loading && comments.length === 0" class="space-y-4">
      <div v-for="i in 3" :key="i" class="animate-pulse flex space-x-3">
        <div class="w-10 h-10 bg-dark-200 dark:bg-dark-700 rounded-full"></div>
        <div class="flex-1">
          <div class="h-4 bg-dark-200 dark:bg-dark-700 rounded w-1/4 mb-2"></div>
          <div class="h-3 bg-dark-200 dark:bg-dark-700 rounded w-3/4"></div>
        </div>
      </div>
    </div>
    
    <div v-else-if="comments.length === 0" class="text-center py-8 text-dark-500 dark:text-dark-400">
      No comments yet. Be the first to share your thoughts!
    </div>
    
    <div v-else class="space-y-6">
      <div v-for="comment in comments" :key="comment.id" class="flex space-x-3 animate-fadeIn">
        <div class="w-10 h-10 bg-primary-500 rounded-full flex items-center justify-center text-white font-medium flex-shrink-0">
          {{ comment.author?.username?.charAt(0).toUpperCase() }}
        </div>
        <div class="flex-1">
          <div class="flex items-center justify-between">
            <div class="flex items-center space-x-2">
              <span class="font-medium text-dark-900 dark:text-white">{{ comment.author?.username }}</span>
              <span class="text-sm text-dark-500 dark:text-dark-400">{{ formatDate(comment.createdAt) }}</span>
            </div>
            <button 
              v-if="currentUserId === comment.author?.id"
              @click="deleteComment(comment.id)"
              class="text-dark-400 hover:text-red-500 transition-colors"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>
          <p class="mt-1 text-dark-700 dark:text-dark-300">{{ comment.content }}</p>
        </div>
      </div>
      
      <button 
        v-if="hasMore"
        @click="loadComments()"
        :disabled="loading"
        class="btn-secondary w-full"
      >
        {{ loading ? 'Loading...' : 'Load More' }}
      </button>
    </div>
  </section>
</template>
