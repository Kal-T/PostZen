import axios from 'axios'

const instance = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json'
    }
})

let authToken = null
let refreshTokenFn = null

// Request interceptor for adding auth header
instance.interceptors.request.use(
    (config) => {
        if (authToken) {
            config.headers.Authorization = `Bearer ${authToken}`
        }
        return config
    },
    (error) => Promise.reject(error)
)

// Response interceptor for handling 401 errors
instance.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config

        if (error.response?.status === 401 && !originalRequest._retry && refreshTokenFn) {
            originalRequest._retry = true
            try {
                const newToken = await refreshTokenFn()
                originalRequest.headers.Authorization = `Bearer ${newToken}`
                return instance(originalRequest)
            } catch (e) {
                return Promise.reject(error)
            }
        }

        return Promise.reject(error)
    }
)

const api = {
    setAuthToken(token) {
        authToken = token
    },

    setRefreshTokenFn(fn) {
        refreshTokenFn = fn
    },

    // Auth
    login: (data) => instance.post('/auth/login', data).then(r => r.data),
    register: (data) => instance.post('/auth/register', data).then(r => r.data),
    logout: (refreshToken) => instance.post('/auth/logout', { refreshToken }).then(r => r.data),
    refreshToken: (refreshToken) => instance.post('/auth/refresh', { refreshToken }).then(r => r.data),
    forgotPassword: (email) => instance.post('/auth/forgot-password', { email }).then(r => r.data),
    resetPassword: (token, newPassword) => instance.post('/auth/reset-password', { token, newPassword }).then(r => r.data),

    // Posts
    getPosts: (page = 0, size = 10) => instance.get(`/posts?page=${page}&size=${size}`).then(r => r.data),
    getPostsByAuthor: (authorId, page = 0, size = 10) => instance.get(`/posts/author/${authorId}?page=${page}&size=${size}`).then(r => r.data),
    getPost: (slug) => instance.get(`/posts/${slug}`).then(r => r.data),
    createPost: (data) => instance.post('/posts', data).then(r => r.data),
    updatePost: (id, data) => instance.put(`/posts/${id}`, data).then(r => r.data),
    deletePost: (id) => instance.delete(`/posts/${id}`).then(r => r.data),

    // Comments
    getComments: (postId, page = 0, size = 20) => instance.get(`/posts/${postId}/comments?page=${page}&size=${size}`).then(r => r.data),
    createComment: (postId, content) => instance.post(`/posts/${postId}/comments`, { content }).then(r => r.data),
    deleteComment: (id) => instance.delete(`/comments/${id}`).then(r => r.data),

    // Files
    uploadFile: (file) => {
        const formData = new FormData()
        formData.append('file', file)
        return instance.post('/files/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        }).then(r => r.data)
    }
}

export default api
