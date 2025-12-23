import { createRouter, createWebHistory } from 'vue-router';
import { useToastStore } from '@/stores/toast';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'main', component: () => import('@/views/rank/Rank.vue') },
    { path: '/login', name: 'login', component: () => import('@/views/auth/LoginView.vue') },
    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: () => import('@/views/auth/ForgotPasswordView.vue'),
    },
    { path: '/signup', name: 'signup', component: () => import('@/views/auth/SignupView.vue') },

    { path: '/rank', name: 'rank', component: () => import('@/views/rank/Rank.vue') },
    { path: '/dashboard', name: 'dashboard', component: () => import('@/views/dashboard/Dashboard.vue') },
    { path: '/articles', name: 'articles', component: () => import('@/views/article/Article.vue') },
    { path: '/articles/:id', name: 'article-detail', component: () => import('@/views/article/ArticleDetail.vue') },
    {
      path: '/myfarm',
      name: 'myfarm',
      component: () => import('@/views/myfarm/MyFarm.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/ai-chat',
      name: 'ai-chat',
      component: () => import('@/views/ai/Chatbot.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('@/views/main/MainPage.vue'),
      meta: { hideSidebar: true },
    },
    {
      path: '/mypage',
      component: () => import('@/views/mypage/MyPage.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: 'password', name: 'mypage-password', component: () => import('@/views/mypage/PasswordChange.vue') },
      ],
    },
  ],
});

router.beforeEach((to, from, next) => {
  const accessToken = localStorage.getItem('accessToken');
  const toastStore = useToastStore();

  const hasValidToken =
    accessToken && accessToken.trim() !== '' && accessToken !== 'null' && accessToken !== 'undefined';

  const isAuthRequired = to.matched.some((record) => record.meta.requiresAuth);

  if (isAuthRequired && !hasValidToken) {
    next({ name: 'login' });
    Promise.resolve().then(() => toastStore.show('로그인이 필요한 기능입니다', 'info'));
    return;
  }

  if ((to.name === 'login' || to.name === 'signup') && hasValidToken) {
    next({ name: 'main' });
    return;
  }

  next();
});

export default router;
