<template>
  <div class="flex h-screen overflow-hidden flex-col" :class="$route.meta.hideSidebar">
    <div class="flex flex-1 overflow-hidden">
      <SideBar v-if="!$route.meta.hideSidebar" />

      <main class="flex-1 overflow-y-auto flex flex-col bg-gray-50" :class="{ 'w-full': $route.meta.hideSidebar }">
        <div class="flex-1">
          <RouterView />
        </div>

        <footer v-if="!$route.meta.hideFooter" class="mt-20 border-t border-gray-200 bg-white">
          <Footer />
        </footer>
      </main>
    </div>
  </div>

  <div v-if="toast.visible" class="toast" :class="toast.type">
    {{ toast.message }}
  </div>

  <AlertModal
    :show="alert.show"
    :title="alert.title"
    :message="alert.message"
    @confirm="alert.handleConfirm"
  />
</template>

<script setup>
import { onMounted } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useToastStore } from '@/stores/toast';
import { useAlertStore } from '@/stores/alert';
import SideBar from '@/components/layout/SideBar.vue';
import Footer from '@/components/layout/Footer.vue';
import AlertModal from '@/components/modal/AlertModal.vue';

const { checkAuth } = useAuthStore();
const toast = useToastStore();
const alert = useAlertStore();

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
