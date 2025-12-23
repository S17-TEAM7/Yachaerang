<template>
  <div class="flex h-screen w-64 flex-col border-r border-gray-200 bg-white">
    <!-- Logo/Header -->
    <div class="flex h-16 items-center border-b border-gray-200 px-6 cursor-pointer" @click="goToMain">
      <BrandLogo :src="logoUrl" size="sm" />
    </div>
    <!-- Navigation -->
    <nav class="flex-1 flex flex-col p-4">
      <div class="space-y-1">
        <router-link v-for="item in navigation" :key="item.name" :to="item.href" custom v-slot="{ navigate, isActive }">
          <div
            @click="navigate"
            class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors cursor-pointer"
            :class="isActive ? 'bg-[#F44323] text-white' : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'"
          >
            <component :is="item.icon" class="h-5 w-5" />
            {{ item.name }}
          </div>
        </router-link>
      </div>

      <div class="mt-auto pt-4 border-t border-gray-100">
        <router-link to="/" custom v-slot="{ navigate, isActive }">
          <div
            @click="navigate"
            class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors cursor-pointer"
            :class="isActive ? 'bg-[#F44323] text-white' : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'"
          >
            <IconInfo class="h-5 w-5" />
            서비스 소개
          </div>
        </router-link>
      </div>
    </nav>

    <!-- User Profile -->
    <div class="border-t border-gray-200 p-4">
      <!-- 로그인 상태일 때 -->
      <template v-if="isLoggedIn">
        <div
          @click="goToMyPage"
          class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors cursor-pointer mb-2"
          :class="isProfileActive ? 'bg-[#F44323] text-white' : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'"
        >
          <img
            v-if="props.userAvatar"
            :src="props.userAvatar"
            class="h-5 w-5 rounded-full object-cover"
            alt="Profile"
            @error="handleAvatarError"
          />
          <IconUser v-else class="h-5 w-5" />
          My Page
        </div>
        <div
          @click="handleLogout"
          class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors cursor-pointer text-gray-600 hover:bg-gray-100 hover:text-gray-900"
        >
          <IconLogout class="h-5 w-5" />
          로그아웃
        </div>
      </template>
      <!-- 로그인 상태가 아닐 때 -->
      <div
        v-else
        @click="goToLogin"
        class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors cursor-pointer text-gray-600 hover:bg-gray-100 hover:text-gray-900"
      >
        <IconLogin class="h-5 w-5" />
        로그인
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { tokenStorage } from '@/utils/storage';

import BrandLogo from '../brand/BrandLogo.vue';
import logoUrl from '../../assets/logo.svg';

import IconDashboard from '../icons/IconDashboard.vue';
import IconTrendingUp from '../icons/IconTrendingUp.vue';
import IconSprout from '../icons/IconSprout.vue';
import IconNewspaper from '../icons/IconNewspaper.vue';
import IconMessage from '../icons/IconMessage.vue';
import IconInfo from '../icons/IconInfo.vue';
import IconUser from '../icons/IconUser.vue';
import IconLogin from '../icons/IconLogin.vue';
import IconLogout from '../icons/IconLogout.vue';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const props = defineProps({
  userAvatar: { type: String, default: '' },
});

const navigation = [
  { name: '대시보드', href: '/dashboard', icon: IconDashboard },
  { name: '랭킹', href: '/rank', icon: IconTrendingUp },
  { name: '나의 농장', href: '/myfarm', icon: IconSprout },
  { name: '농촌 기사', href: '/articles', icon: IconNewspaper },
  { name: '챗봇 야치', href: '/ai-chat', icon: IconMessage },
];

// 로그인 상태 확인
const isLoggedIn = computed(() => {
  return tokenStorage.hasTokens();
});

// 프로필 활성화 상태 확인
const isProfileActive = computed(() => route.path === '/mypage');

// 메인으로 이동
const goToMain = () => {
  window.location.href = '/';
};

// 마이페이지로 이동
const goToMyPage = () => {
  window.location.href = '/mypage';
};

// 로그인 페이지로 이동
const goToLogin = () => {
  router.push('/login');
};

// 로그아웃 처리
const handleLogout = async () => {
  await authStore.logout();
  const currentPath = route.path;

  const keepPages = ['/dashboard', '/rank', '/articles'];

  if (keepPages.includes(currentPath)) {
    window.location.href = currentPath;
  } else {
    window.location.href = '/login';
  }
};

// 이미지 에러 핸들러
const handleImageError = (e) => {
  e.target.style.display = 'none';
};

const handleAvatarError = (e) => {
  e.target.style.display = 'none';
};
</script>
