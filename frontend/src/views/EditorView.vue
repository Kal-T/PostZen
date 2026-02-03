<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import api from '../services/api'

const route = useRoute()
const router = useRouter()

const title = ref('')
const content = ref('')
const status = ref('DRAFT')
const scheduledAt = ref('')
const loading = ref(false)
const saving = ref(false)
const error = ref(null)
const showPreview = ref(false)

const isEditing = computed(() => !!route.params.id)

const renderedContent = computed(() => {
  if (!content.value) return '<p class="text-dark-400">Start writing to see preview...</p>'
  const html = marked(content.value)
  return DOMPurify.sanitize(html)
})

async function loadPost() {
  if (!route.params.id) return
  
  try {
    loading.value = true
    const post = await api.getPost(route.params.id)
    title.value = post.title
    content.value = post.content
    status.value = post.status
    if (post.scheduledAt) {
      scheduledAt.value = new Date(post.scheduledAt).toISOString().slice(0, 16)
    }
  } catch (e) {
    error.value = 'Failed to load post'
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function handleSave(publishStatus = null) {
  if (!title.value.trim() || !content.value.trim()) {
    error.value = 'Title and content are required'
    return
  }
  
  try {
    saving.value = true
    error.value = null
    
    const data = {
      title: title.value,
      content: content.value,
      status: publishStatus || status.value,
      scheduledAt: status.value === 'SCHEDULED' && scheduledAt.value 
        ? new Date(scheduledAt.value).toISOString() 
        : null
    }
    
    let post
    if (isEditing.value) {
      post = await api.updatePost(route.params.id, data)
    } else {
      post = await api.createPost(data)
    }
    
    router.push(`/post/${post.slug}`)
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to save post'
    console.error(e)
  } finally {
    saving.value = false
  }
}

async function handleImageUpload(event) {
  const file = event.target.files[0]
  if (!file) return
  
  try {
    const response = await api.uploadFile(file)
    const imageMarkdown = `![${response.originalName}](${response.url})`
    content.value += `\n${imageMarkdown}\n`
  } catch (e) {
    error.value = 'Failed to upload image'
    console.error(e)
  }
}

onMounted(() => {
  loadPost()
})
</script>

<template>
  <div class="max-w-6xl mx-auto px-4 py-8">
    <div v-if="loading" class="text-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto"></div>
    </div>
    
    <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Editor Panel -->
      <div class="space-y-4">
        <div class="flex items-center justify-between">
          <h1 class="text-2xl font-bold text-dark-900 dark:text-white">
            {{ isEditing ? 'Edit Post' : 'Write a Story' }}
          </h1>
          <button 
            @click="showPreview = !showPreview"
            class="lg:hidden btn-secondary"
          >
            {{ showPreview ? 'Edit' : 'Preview' }}
          </button>
        </div>
        
        <div :class="{ 'hidden lg:block': showPreview }">
          <input
            v-model="title"
            type="text"
            placeholder="Post title..."
            class="input text-2xl font-bold"
          />
          
          <div class="mt-4">
            <div class="flex items-center space-x-2 mb-2">
              <label class="btn-secondary cursor-pointer">
                <input 
                  type="file" 
                  accept="image/*" 
                  @change="handleImageUpload"
                  class="hidden"
                />
                📷 Add Image
              </label>
            </div>
            
            <textarea
              v-model="content"
              placeholder="Write your story in Markdown..."
              rows="20"
              class="input font-mono text-sm resize-none"
            ></textarea>
          </div>
          
          <div class="mt-4 space-y-4">
            <div class="flex items-center space-x-4">
              <select v-model="status" class="input w-auto">
                <option value="DRAFT">Draft</option>
                <option value="PUBLISHED">Publish Now</option>
                <option value="SCHEDULED">Schedule</option>
              </select>
              
              <input
                v-if="status === 'SCHEDULED'"
                v-model="scheduledAt"
                type="datetime-local"
                class="input w-auto"
              />
            </div>
            
            <div v-if="error" class="p-3 bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-lg text-red-600 dark:text-red-400 text-sm">
              {{ error }}
            </div>
            
            <div class="flex space-x-3">
              <button 
                @click="handleSave('DRAFT')"
                :disabled="saving"
                class="btn-secondary"
              >
                Save Draft
              </button>
              <button 
                @click="handleSave(status === 'SCHEDULED' ? 'SCHEDULED' : 'PUBLISHED')"
                :disabled="saving"
                class="btn-primary flex items-center"
              >
                <svg v-if="saving" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                {{ status === 'SCHEDULED' ? 'Schedule' : 'Publish' }}
              </button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Preview Panel -->
      <div :class="{ 'hidden lg:block': !showPreview }">
        <div class="card p-6 sticky top-24">
          <h2 class="text-sm font-medium text-dark-500 dark:text-dark-400 mb-4">Preview</h2>
          <h1 class="text-2xl font-bold text-dark-900 dark:text-white mb-4">
            {{ title || 'Untitled' }}
          </h1>
          <div class="prose dark:prose-dark" v-html="renderedContent"></div>
        </div>
      </div>
    </div>
  </div>
</template>
