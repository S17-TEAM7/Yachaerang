<template>
  <div class="flex h-screen overflow-hidden flex-col" :class="$route.meta.hideSidebar">
    <div class="flex flex-1 overflow-hidden">
      <SideBar v-if="!$route.meta.hideSidebar" />
      <main class="flex-1 overflow-y-auto flex flex-col" :class="{ 'w-full': $route.meta.hideSidebar }">
        <div class="flex-1 min-h-0">
          <RouterView />
        </div>
      </main>
    </div>
  </div>

  <!-- Toast -->
  <div v-if="toast.visible" class="toast" :class="toast.type">
    {{ toast.message }}
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useToastStore } from '@/stores/toast';
import SideBar from '@/components/layout/SideBar.vue';
import Footer from '@/components/layout/Footer.vue';

const { checkAuth } = useAuthStore();
const toast = useToastStore();

onMounted(() => {
  checkAuth();
});
</script>

<style>
.toast {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 24px;
  border-radius: 8px;
  color: white;
  font-size: 14px;
  z-index: 9999;
}

.toast.success {
  background-color: #10b981;
}

.toast.error {
  background-color: #ef4444;
}

.toast.warning {
  background-color: #f59e0b;
}

.toast.info {
  background-color: #3b82f6;
}
</style>
