<template>
  <main class="flex-1 overflow-y-auto bg-gray-50">
    <div v-if="!isPasswordPage" class="max-w-7xl mx-auto px-4 sm:px-6 pt-4 md:pt-6 w-full">
      <PageHeader title="마이페이지" description="나의 정보를 관리해보세요." />
    </div>

    <div class="max-w-7xl mx-auto px-4 sm:px-6 w-full space-y-6">

      <router-view v-if="isChildRoute" />

      <template v-else>
        <ProfileSection
          :profile="profile"
          @update:profile="handleProfileUpdate"
        />

        <AccountActions />

        <div class="grid gap-6 md:grid-cols-2">
          <FavoriteDashboards />
          <FavoriteArticles />
        </div>
      </template>
    </div>
  </main>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';

import ProfileSection from './components/ProfileSection.vue';
import AccountActions from './components/AccountActions.vue';
import FavoriteDashboards from '@/views/mypage/components/FavoriteDashboards.vue';
import FavoriteArticles from '@/views/mypage/components/FavoriteArticles.vue';
import PageHeader from '@/components/layout/PageHeader.vue';

const route = useRoute();

const isChildRoute = computed(() => route.path !== '/mypage');
const isPasswordPage = computed(() => route.path === '/mypage/password');
</script>