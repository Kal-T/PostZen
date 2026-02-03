import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
    {
        path: '/',
        name: 'home',
        component: () => import('../views/HomeView.vue')
    },
    {
        path: '/login',
        name: 'login',
        component: () => import('../views/LoginView.vue'),
        meta: { guest: true }
    },
    {
        path: '/register',
        name: 'register',
        component: () => import('../views/RegisterView.vue'),
        meta: { guest: true }
    },
    {
        path: '/forgot-password',
        name: 'forgot-password',
        component: () => import('../views/ForgotPasswordView.vue'),
        meta: { guest: true }
    },
    {
        path: '/post/:slug',
        name: 'post',
        component: () => import('../views/PostView.vue')
    },
    {
        path: '/write',
        name: 'write',
        component: () => import('../views/EditorView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/edit/:id',
        name: 'edit',
        component: () => import('../views/EditorView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/profile',
        name: 'profile',
        component: () => import('../views/ProfileView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/:pathMatch(.*)*',
        name: 'not-found',
        component: () => import('../views/NotFoundView.vue')
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes,
    scrollBehavior(to, from, savedPosition) {
        if (savedPosition) {
            return savedPosition
        }
        return { top: 0 }
    }
})

// Navigation guards
router.beforeEach(async (to, from, next) => {
    const authStore = useAuthStore()

    // Initialize auth state from localStorage
    if (!authStore.initialized) {
        await authStore.init()
    }

    // Protected routes
    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
        next({ name: 'login', query: { redirect: to.fullPath } })
        return
    }

    // Guest-only routes (redirect to home if logged in)
    if (to.meta.guest && authStore.isAuthenticated) {
        next({ name: 'home' })
        return
    }

    next()
})

export default router
